package com.example.backend.auth.controller;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.application.CurrentPasswordRejectedException;
import com.example.backend.auth.application.LoginIdentityService;
import com.example.backend.auth.application.LoginService;
import com.example.backend.auth.application.LoginService.LoginOutcome;
import com.example.backend.auth.application.PasswordChangeService;
import com.example.backend.auth.application.PasswordPolicyViolationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.CookieSerializer.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Spring Security's prefix on every role-derived authority. */
    private static final String ROLE_PREFIX = "ROLE_";

    /** The role name the SPA reads as "may reach the administrative interface". */
    private static final String ADMIN_ROLE = "ADMIN";

    /** The authority the Admin group's membership confers, as Spring Security spells it. */
    private static final String ADMIN_AUTHORITY = ROLE_PREFIX + ADMIN_ROLE;

    private final LoginService login;
    private final PasswordChangeService passwordChanges;
    private final AuditTrail audit;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;
    private final CookieSerializer cookieSerializer;

    public AuthController(
            LoginService login,
            PasswordChangeService passwordChanges,
            AuditTrail audit,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            CsrfTokenRepository csrfTokenRepository,
            CookieSerializer cookieSerializer) {
        this.login = login;
        this.passwordChanges = passwordChanges;
        this.audit = audit;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.csrfTokenRepository = csrfTokenRepository;
        this.cookieSerializer = cookieSerializer;
    }

    /**
     * Turns submitted credentials into a session. What counts as a successful
     * login — including the failure run a refusal lengthens — is
     * {@link LoginService}'s; everything below it here is the session and CSRF
     * work that only a web adapter can do.
     */
    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        LoginOutcome outcome = login.logIn(body.username(), body.password());
        Authentication authentication = outcome.authentication();

        // Rotate before the context is saved, so the authentication lands in the
        // session the caller will keep using rather than the pre-login one.
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        // Overrides Spring Session's default principal-index population (which
        // reads Authentication.getName(), i.e. the userName) with the SCIM
        // stable id, so AccountSessionsAdapter — and any future stable-id-keyed
        // session lookup — finds this session by an id that survives a later
        // username change. Authentication.getName() itself is untouched: the
        // security context still names the account by username, which is what
        // userResponse() below reports.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute(
                    FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                    outcome.userId().toString());
        }

        issueCsrfToken(request, response);

        return userResponse(authentication);
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return userResponse(authentication);
    }

    @DeleteMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            // Recorded before the session is invalidated, and from the session
            // itself: the principal index holds the account's stable id, which is
            // what an event may name, while the security context names it by
            // username, which is what an event may not. Fail-closed, so a logout
            // this service cannot account for leaves the session standing rather
            // than ending it silently.
            recordLogout(session);
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        // Invalidating the session server-side leaves the browser holding a
        // cookie that now names nothing. Expire it so a later request arrives
        // without a session id at all.
        cookieSerializer.writeCookieValue(new CookieValue(request, response, ""));

        issueCsrfToken(request, response);
    }

    /**
     * Records the logout against the account the session belongs to.
     *
     * <p>A session carrying no principal index is one minted before it was signed
     * in to — there is no account to name, and nothing was logged out — so nothing
     * is recorded rather than an event with an invented subject.
     */
    private void recordLogout(HttpSession session) {
        if (session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME)
                instanceof String userId) {
            audit.recordLogout(UUID.fromString(userId));
        }
    }

    /**
     * Replaces the CSRF cookie with a freshly minted token. On login this stops a
     * token minted before authentication from remaining valid after it; on logout
     * it both discards the token that belonged to the closed session and leaves
     * the caller with a usable token, so the next login can be submitted without
     * a round trip to fetch one.
     */
    private void issueCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(
                csrfTokenRepository.generateToken(request), request, response);
    }

    /**
     * The single role the SPA is told the caller has.
     *
     * <p>{@code ADMIN} wins when it is present, and that is stated rather than left to the order
     * the authorities happen to arrive in. Authority is now DERIVED: an administrator holds
     * {@code ROLE_ADMIN} and {@code ROLE_USER} both, because baseline access is what being an
     * active identity means and administrative access is what the Admin group adds. A reader that
     * took the first authority would report an administrator as an ordinary user whenever the
     * ordering changed, which is the kind of defect that surfaces as "the admin screens vanished"
     * long after the commit that caused it.
     */
    private UserResponse userResponse(Authentication authentication) {
        boolean changeRequired = authentication.getAuthorities().stream()
                .anyMatch(authority -> LoginIdentityService.PASSWORD_CHANGE_REQUIRED_AUTHORITY
                        .equals(authority.getAuthority()));
        if (changeRequired) {
            // Confined: the session holds no role at all until the credential is replaced, so it
            // reports none rather than one it cannot exercise.
            return new UserResponse(authentication.getName(), null, true);
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
        if (admin) {
            return new UserResponse(authentication.getName(), ADMIN_ROLE, false);
        }
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated identity has no role"));
        return new UserResponse(authentication.getName(), role, false);
    }

    /**
     * The self-service password change: the one capability besides logging out that a session
     * confined by a required change holds, and open to every authenticated session.
     *
     * <p>The User is the one the SESSION belongs to, read from the stable id its principal index
     * holds — never from the request — so there is no identifier to tamper with. On success every
     * session of that User has been revoked after the commit, this one included; it is also
     * invalidated here, directly, so the servlet container's copy cannot be written back when the
     * request completes, and the cookie is expired so the next request arrives as a stranger.
     */
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME)
                        instanceof String userId)) {
            throw new CurrentPasswordRejectedException();
        }
        passwordChanges.changePassword(
                UUID.fromString(userId), body.currentPassword(), body.newPassword());

        session.invalidate();
        SecurityContextHolder.clearContext();
        cookieSerializer.writeCookieValue(new CookieValue(request, response, ""));
        issueCsrfToken(request, response);
    }

    /** Wrong current password, lockout, inactive: the same bare {@code 401} Login gives. */
    @ExceptionHandler(CurrentPasswordRejectedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void passwordChangeRejected() {
    }

    /** The unmet rule, by name and description; never either submitted value. */
    @ExceptionHandler(PasswordPolicyViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public PasswordRuleViolation passwordPolicyViolated(PasswordPolicyViolationException violation) {
        return new PasswordRuleViolation(violation.ruleName(), violation.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void authenticationFailed() {
        // Deliberately omit details so callers cannot distinguish unknown users.
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /**
     * Current and new password. {@link #toString()} is overridden because a record's generated one
     * would print both, and a request body is exactly what reaches a log through a debugger or a
     * validation message.
     */
    public record ChangePasswordRequest(
            @NotBlank String currentPassword, @NotBlank String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordRequest[redacted]";
        }
    }

    /** A {@code 400} for a new password breaking a policy rule. */
    public record PasswordRuleViolation(String rule, String message) {
    }

    /**
     * @param role                   {@code USER} or {@code ADMIN}; {@code null} while a password
     *                               change is required, because the session holds neither
     * @param passwordChangeRequired whether the session is confined to the change flow
     */
    public record UserResponse(String username, String role, boolean passwordChangeRequired) {
    }
}
