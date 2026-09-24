package com.example.backend.auth.config;

import com.example.backend.web.SpaRoutes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
public class SecurityConfig {

    /**
     * The single-page application loads only same-origin module scripts and a
     * same-origin stylesheet, so everything can be locked to {@code 'self'}.
     * Inline styles stay allowed because component libraries inject style
     * elements at runtime; inline script is not allowed at all.
     */
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    private static final String PERMISSIONS_POLICY = String.join(", ",
            "geolocation=()",
            "camera=()",
            "microphone=()",
            "payment=()",
            "usb=()");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Session fixation protection for the login path. The filter chain cannot
     * apply this itself: nothing in the chain authenticates, so no chain-level
     * {@code sessionFixation()} setting would ever run. AuthController invokes
     * this strategy instead, which rotates the id of a session the caller
     * already held before authenticating.
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    /**
     * Shared with AuthController for the same reason as the security context
     * repository: the chain validates the token on every unsafe request, and the
     * login and logout paths issue a fresh one, so both halves must read and
     * write the same cookie through the same configuration.
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) ->
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return http
                // spa() installs the request handler that reads the raw token
                // value back from the X-XSRF-TOKEN header while still masking
                // the value it writes into request attributes. It also creates
                // its own cookie repository, so the shared bean has to be set
                // afterwards to win.
                .csrf(csrf -> csrf
                        .spa()
                        .csrfTokenRepository(csrfTokenRepository))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                // Share one repository with AuthController: the login path writes the
                // authentication here and every later request reads it back from the
                // same place. Left implicit, the chain builds its own repository.
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy(PERMISSIONS_POLICY)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorized))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/actuator/health").permitAll()
                        .requestMatchers("/api/accounts", "/api/accounts/**").hasRole("ADMIN")
                        .requestMatchers(this::isFrontendGet).permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    /**
     * Which paths the frontend owns is SpaRoutes' knowledge; that only a GET of
     * one may skip authentication is this chain's.
     */
    private boolean isFrontendGet(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return HttpMethod.GET.matches(request.getMethod())
                && !SpaRoutes.isReservedServerPath(path);
    }
}
