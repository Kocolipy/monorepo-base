package com.example.backend.scim.config;

import com.example.backend.scim.application.ConnectorAuthenticationService;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * The SCIM namespace's own security chain: stateless, bearer-authenticated, and
 * ordered ahead of the application chain.
 *
 * <p>A second chain rather than more rules in the first one, because the two
 * namespaces disagree about nearly everything a chain configures. The application
 * chain is session-backed with CSRF double-submit and an SPA fallback; the SCIM chain
 * is a stateless API authenticated by a bearer credential on every request. Folding
 * the second into the first would mean every rule carrying a path condition, and the
 * SPA's session behaviour reaching an endpoint no browser calls.
 *
 * <p><strong>Ordering is load-bearing.</strong> This chain declares a
 * {@code securityMatcher}; the application chain matches everything and must
 * therefore be last, or it would answer SCIM requests with the SPA's rules — and
 * {@code SpaRoutes} would treat an unknown SCIM path as a client-side route and
 * return the HTML shell with a {@code 200}. {@code /scim} is a reserved server path
 * for that reason, so the two mechanisms agree even if the ordering is ever changed.
 */
@Configuration
public class ScimSecurityConfig {

    /** Ahead of the application chain, which matches every remaining request. */
    public static final int SCIM_CHAIN_ORDER = 1;

    /** The namespace this chain owns, exact path and everything beneath it. */
    private static final String SCIM_NAMESPACE = "/scim/v2/**";

    /**
     * Discovery, which a connector must read before it holds a token at all — that is
     * the point of the endpoints, so they are public. They disclose this service's
     * SCIM capabilities and schemas and no directory content; GET only, so nothing
     * public can write. The handlers arrive with the inbound adapter ticket; the rules
     * are here now because the chain they belong to is.
     */
    private static final String[] PUBLIC_DISCOVERY_PATHS = {
        "/scim/v2/ServiceProviderConfig",
        "/scim/v2/ResourceTypes",
        "/scim/v2/ResourceTypes/**",
        "/scim/v2/Schemas",
        "/scim/v2/Schemas/**",
    };

    /**
     * The one source of random material for connector tokens.
     *
     * <p>The no-argument constructor, which yields the platform's strongest seeded
     * generator and reseeds itself; {@code SecureRandom.getInstanceStrong()} is
     * deliberately not used, because on Linux it can block on entropy and would turn
     * minting a token into an unbounded wait on a request thread.
     */
    @Bean
    public SecureRandom connectorTokenRandom() {
        return new SecureRandom();
    }

    /**
     * Ahead of the application chain, which matches every remaining request.
     *
     * <p>The annotation goes on the {@code @Bean} METHOD and not on this class:
     * Spring Security orders {@code SecurityFilterChain} beans by their own
     * {@code @Order}, and a class-level one leaves the bean at lowest precedence —
     * which registers the catch-all chain first and makes this one unreachable. That
     * is a startup failure rather than a silent misconfiguration, and
     * {@code ScimSecurityChainOrderTests} pins the resulting order so a future edit
     * cannot invert it back.
     */
    @Bean
    @Order(ScimSecurityConfig.SCIM_CHAIN_ORDER)
    public SecurityFilterChain scimSecurityFilterChain(
            HttpSecurity http, ConnectorAuthenticationService connectors) throws Exception {
        AuthenticationEntryPoint challenge =
                (request, response, exception) -> ScimBearerChallenge.missingCredential(response);

        // A stateless bearer API has no CSRF exposure to protect, because the
        // credential is not ambient. A browser cannot make an authenticated SCIM
        // request on a user's behalf: there is no cookie or session this chain
        // accepts, and the token has to be placed in a header by the caller, which a
        // cross-site form cannot do. The application chain's double-submit is
        // untouched and still guards every cookie-authenticated path — which is what
        // be-csrf-disabled is about, and why this is the one place it does not apply.
        // Written as its own statement so the suppression sits on the flagged line
        // rather than several lines above a builder chain.
        http.csrf(csrf -> csrf.disable()); // nosemgrep: be-csrf-disabled

        return http
                .securityMatcher(SCIM_NAMESPACE)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .anonymous(anonymous -> anonymous.disable())
                // Request-scoped rather than session-backed: an authenticated SCIM
                // request leaves nothing behind, so a token cannot be exchanged for a
                // longer-lived session by presenting it once.
                .securityContext(context -> context
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        new ScimBearerAuthenticationFilter(connectors), AuthorizationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(challenge))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, PUBLIC_DISCOVERY_PATHS).permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
