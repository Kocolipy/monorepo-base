package com.example.backend.auth.controller;

import com.example.backend.auth.application.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
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

    private final LoginService login;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;
    private final CookieSerializer cookieSerializer;

    public AuthController(
            LoginService login,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            CsrfTokenRepository csrfTokenRepository,
            CookieSerializer cookieSerializer) {
        this.login = login;
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
        Authentication authentication = login.logIn(body.username(), body.password());

        // Rotate before the context is saved, so the authentication lands in the
        // session the caller will keep using rather than the pre-login one.
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

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

    private UserResponse userResponse(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated account has no role"));
        return new UserResponse(authentication.getName(), role);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public void authenticationFailed() {
        // Deliberately omit details so callers cannot distinguish unknown users.
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UserResponse(String username, String role) {
    }
}
