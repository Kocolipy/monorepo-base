package com.example.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.web.http.DefaultCookieSerializer;

class AuthControllerTests {

    /** Mirrors {@code server.servlet.session.cookie.name}. */
    private static final String SESSION_COOKIE = "JSESSIONID";

    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    private AuthController controller;

    private CsrfTokenRepository csrfTokenRepository;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder passwordEncoder = config.passwordEncoder();
        UserDetailsService users = config.userDetailsService(
                "ada",
                "correct-password",
                "grace",
                "another-correct-password",
                passwordEncoder);
        AuthenticationManager manager = config.authenticationManager(users, passwordEncoder);
        csrfTokenRepository = config.csrfTokenRepository();
        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName(SESSION_COOKIE);
        controller = new AuthController(
                manager,
                config.securityContextRepository(),
                config.sessionAuthenticationStrategy(),
                csrfTokenRepository,
                cookieSerializer);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginStoresAuthenticatedUserInHttpSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        AuthController.UserResponse response = controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                request,
                new MockHttpServletResponse());

        SecurityContext savedContext = (SecurityContext) request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(response.username()).isEqualTo("ada");
        assertThat(savedContext.getAuthentication().isAuthenticated()).isTrue();
        assertThat(savedContext.getAuthentication().getName()).isEqualTo("ada");
    }

    @Test
    void secondaryUserCanLogIn() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        AuthController.UserResponse response = controller.login(
                new AuthController.LoginRequest("grace", "another-correct-password"),
                request,
                new MockHttpServletResponse());

        SecurityContext savedContext = (SecurityContext) request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(response.username()).isEqualTo("grace");
        assertThat(savedContext.getAuthentication().isAuthenticated()).isTrue();
        assertThat(savedContext.getAuthentication().getName()).isEqualTo("grace");
    }

    @Test
    void loginRejectsInvalidCredentialsWithoutCreatingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> controller.login(
                new AuthController.LoginRequest("ada", "wrong-password"),
                request,
                new MockHttpServletResponse()))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(request.getSession(false)).isNull();
    }

    /**
     * Session fixation protection: a caller that already holds a session gets a
     * new session id once it authenticates, so an id captured before login cannot
     * be replayed against the authenticated session.
     */
    @Test
    void loginRotatesTheSessionIdOfAPreExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String preLoginSessionId = request.getSession(true).getId();

        controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                request,
                new MockHttpServletResponse());

        assertThat(request.getSession(false).getId()).isNotEqualTo(preLoginSessionId);
    }

    /**
     * Rotation has to happen before the authentication is written down, otherwise
     * the context is saved into the session that is about to be replaced and the
     * caller comes back authenticated as nobody.
     */
    @Test
    void loginKeepsTheAuthenticationInTheRotatedSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true);

        controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                request,
                new MockHttpServletResponse());

        SecurityContext savedContext = (SecurityContext) request.getSession(false).getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(savedContext).isNotNull();
        assertThat(savedContext.getAuthentication().getName()).isEqualTo("ada");
    }

    /**
     * A CSRF token obtained before logging in must not survive the privilege
     * change, so login answers with a different one.
     */
    @Test
    void loginReplacesACsrfTokenMintedBeforeAuthentication() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CSRF_COOKIE, "token-from-before-login"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                request,
                response);

        Cookie issued = response.getCookie(CSRF_COOKIE);
        assertThat(issued).isNotNull();
        assertThat(issued.getValue())
                .isNotBlank()
                .isNotEqualTo("token-from-before-login");
    }

    /**
     * Saving to the session is not enough: the rest of the request that performed
     * the login also has to see the authentication, which is what publishing it on
     * the {@link SecurityContextHolder} thread-local provides.
     */
    @Test
    void loginPublishesAuthenticationOnTheCurrentThread() {
        controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                new MockHttpServletRequest(),
                new MockHttpServletResponse());

        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        assertThat(current).isNotNull();
        assertThat(current.getName()).isEqualTo("ada");
        assertThat(current.isAuthenticated()).isTrue();
    }

    @Test
    void currentUserReportsThePrincipalName() {
        AuthController.UserResponse response = controller.currentUser(() -> "ada");

        assertThat(response.username()).isEqualTo("ada");
    }

    @Test
    void logoutInvalidatesTheSessionAndClearsTheSecurityContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                request,
                new MockHttpServletResponse());
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertThat(session).isNotNull();

        controller.logout(request, new MockHttpServletResponse());

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * Invalidating the session server-side leaves the browser holding a cookie
     * that names a session which no longer exists, so logout expires it.
     */
    @Test
    void logoutExpiresTheSessionCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                request,
                new MockHttpServletResponse());
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(request, response);

        Cookie cleared = response.getCookie(SESSION_COOKIE);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getValue()).isEmpty();
        assertThat(cleared.getMaxAge()).isZero();
    }

    /**
     * The token that belonged to the closed session is replaced rather than only
     * deleted, so the next login can be submitted without first fetching one.
     */
    @Test
    void logoutReplacesTheCsrfTokenOfTheClosedSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CSRF_COOKIE, "token-from-the-session-being-closed"));
        controller.login(
                new AuthController.LoginRequest("ada", "correct-password"),
                request,
                new MockHttpServletResponse());
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(request, response);

        Cookie issued = response.getCookie(CSRF_COOKIE);
        assertThat(issued).isNotNull();
        assertThat(issued.getValue())
                .isNotBlank()
                .isNotEqualTo("token-from-the-session-being-closed");
    }

    /**
     * Logging out without a session is a no-op rather than a failure, so a caller
     * whose session already expired still gets a clean logout.
     */
    @Test
    void logoutWithoutASessionSucceedsAndStillClearsTheSecurityContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new TestingAuthenticationToken("ada", "correct-password", "ROLE_USER"));
        SecurityContextHolder.setContext(context);

        assertThatNoException()
                .isThrownBy(() -> controller.logout(request, new MockHttpServletResponse()));

        assertThat(request.getSession(false)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
