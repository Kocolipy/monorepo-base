package com.example.backend.auth.controller;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.application.CurrentPasswordRejectedException;
import com.example.backend.auth.application.LoginIdentityService;
import com.example.backend.auth.application.LoginService;
import com.example.backend.auth.application.LoginService.LoginOutcome;
import com.example.backend.auth.application.PasswordChangeService;
import com.example.backend.auth.application.PasswordPolicyViolationException;
import com.example.backend.observability.LogContext;
import com.example.backend.observability.LogEvent;
import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Severity;
import com.example.backend.observability.LogEvent.Type;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
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

    /** The response header a logout asks the browser to clear the origin's data with. */
    static final String CLEAR_SITE_DATA_HEADER = "Clear-Site-Data";

    /** Every data type a signed-out session may have left in the browser. */
    static final String CLEAR_SITE_DATA_ON_LOGOUT = "\"cache\",\"cookies\",\"storage\"";

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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
        // The caller's session as it is stored now, before rotation renames it: it is the one the
        // login continues in, so it is the one session of the User's that the login keeps.
        HttpSession existing = request.getSession(false);
        LoginOutcome outcome = login.logIn(
                body.username(), body.password(), existing == null ? null : existing.getId());
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

        // The session id has just rotated, but its attributes moved with it — the
        // pre-login CSRF token among them. Dropping it here means a token fetched
        // before authentication is refused after it; the SPA fetches a new one.
        csrfTokenRepository.saveToken(null, request, response);

        HttpSession signedIn = request.getSession();
        recordSessionStart(outcome.userId(), signedIn);
        return userResponse(authentication, signedIn);
    }

    /**
     * The operational stream's {@code session-start}: one per session a login signs in.
     *
     * <p>Written here, where the session becomes an authenticated one, rather than on container
     * session creation. The anonymous session {@code GET /api/auth/csrf} mints exists only to
     * hold the token a login submits; it either becomes this session (its id rotated) or idles
     * out unused, so logging its creation would add a record per page load that names no one.
     * Writing it here also means the record can carry the User's stable id, which no
     * creation-time record could.
     *
     * <p>The record names the session by nothing: not its id, nor anything derived from it,
     * because the id is the session's bearer credential.
     */
    private static void recordSessionStart(UUID userId, HttpSession session) {
        try (LogContext.Scope scope = LogContext.userId(userId)) {
            LogEvent.classify(log.atInfo(), Operation.SESSION_START, Category.PROCESS, Type.START)
                    .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                    .addKeyValue(LogEvent.SEVERITY, Severity.LOW.value())
                    .addKeyValue(LogEvent.SESSION_MAX_INACTIVE_INTERVAL,
                            session.getMaxInactiveInterval())
                    .log("Session started");
        }
    }

    /**
     * The CSRF token bound to the caller's session, in the body and never in a
     * cookie: the SPA keeps it in memory and echoes it in the header named here.
     *
     * <p>Public, so a guest can obtain the token its login submission needs; the
     * call creates the session the token belongs to when there is none yet. The
     * value is XOR-masked afresh on every call, so two responses never repeat each
     * other, and {@code no-store} keeps it out of every cache between here and the
     * page that asked.
     */
    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrfToken(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfTokenResponse(token.getHeaderName(), token.getToken()));
    }

    /**
     * The signed-in account, plus the idle bound its session is held to. Reading the session also
     * renews that bound, so the SPA's "stay signed in" is this request.
     */
    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication, HttpSession session) {
        return userResponse(authentication, session);
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

        // Tells the browser to drop what the signed-out session left behind — cached responses,
        // cookies and storage — whether or not a live session arrived with the request, so a
        // caller whose session already expired is cleaned up the same way.
        response.setHeader(CLEAR_SITE_DATA_HEADER, CLEAR_SITE_DATA_ON_LOGOUT);

        // No CSRF work: the token lived in the session just invalidated, so it is
        // already gone, and the next login fetches one for the session it creates.
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
                instanceof String indexed) {
            UUID userId = UUID.fromString(indexed);
            audit.recordLogout(userId);
            // Beside the audit append, after it succeeded: the trail is the record of who
            // logged out, and this is the operational stream's line for the same moment.
            try (LogContext.Scope scope = LogContext.userId(userId)) {
                LogEvent.classify(log.atInfo(), Operation.LOGOUT, Category.PROCESS,
                                Type.USER, Type.END)
                        .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                        .log("Logout completed");
            }
        }
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
     *
     * <p>The idle timeout is read off the session itself rather than out of configuration, so the
     * figure the SPA signs out by is the one this session actually expires by and cannot drift
     * from it.
     */
    private UserResponse userResponse(Authentication authentication, HttpSession session) {
        int idleTimeoutSeconds = session.getMaxInactiveInterval();
        boolean changeRequired = authentication.getAuthorities().stream()
                .anyMatch(authority -> LoginIdentityService.PASSWORD_CHANGE_REQUIRED_AUTHORITY
                        .equals(authority.getAuthority()));
        if (changeRequired) {
            // Confined: the session holds no role at all until the credential is replaced, so it
            // reports none rather than one it cannot exercise.
            return new UserResponse(authentication.getName(), null, true, idleTimeoutSeconds);
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ADMIN_AUTHORITY.equals(authority.getAuthority()));
        if (admin) {
            return new UserResponse(
                    authentication.getName(), ADMIN_ROLE, false, idleTimeoutSeconds);
        }
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated identity has no role"));
        return new UserResponse(authentication.getName(), role, false, idleTimeoutSeconds);
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

    /**
     * Both fields are bounded before anything reads them — the name by the {@code userName}
     * column's limit, the password by the password policy's — so an over-length body is the same
     * bare {@code 400} as a blank one and never reaches a failure run, the audit trail or the
     * password hash.
     */
    public record LoginRequest(
            @NotBlank @MaxUserNameLength String username,
            @NotBlank @MaxPasswordLength String password) {
    }

    /**
     * Current and new password, each bounded like Login's. {@link #toString()} is overridden
     * because a record's generated one would print both, and a request body is exactly what reaches
     * a log through a debugger or a validation message.
     */
    public record ChangePasswordRequest(
            @NotBlank @MaxPasswordLength String currentPassword,
            @NotBlank @MaxPasswordLength String newPassword) {

        @Override
        public String toString() {
            return "ChangePasswordRequest[redacted]";
        }
    }

    /**
     * @param headerName the request header an unsafe request carries the token in
     * @param token      the masked token value to send in it
     */
    public record CsrfTokenResponse(String headerName, String token) {
    }

    /** A {@code 400} for a new password breaking a policy rule. */
    public record PasswordRuleViolation(String rule, String message) {
    }

    /**
     * @param role                   {@code USER} or {@code ADMIN}; {@code null} while a password
     *                               change is required, because the session holds neither
     * @param passwordChangeRequired whether the session is confined to the change flow
     * @param idleTimeoutSeconds     the session's idle bound: how long it survives without a
     *                               request ({@code server.servlet.session.timeout}). The SPA signs
     *                               an inactive user out by this figure, so it never outlasts it
     */
    public record UserResponse(
            String username, String role, boolean passwordChangeRequired, int idleTimeoutSeconds) {
    }
}
