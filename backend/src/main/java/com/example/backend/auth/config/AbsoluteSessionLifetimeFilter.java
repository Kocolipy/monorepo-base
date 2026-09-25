package com.example.backend.auth.config;

import com.example.backend.auth.domain.AbsoluteSessionLifetimePolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces {@link AbsoluteSessionLifetimePolicy} on every request that carries
 * an existing session.
 *
 * <p>Runs before the security context is loaded from the session (see its
 * placement in {@link SecurityConfig#securityFilterChain}), so an expired
 * session is invalidated and its authentication cleared before anything later
 * in the chain reads either — the rest of the chain sees exactly what it would
 * see if the browser had presented no session cookie at all, and a protected
 * path answers with the ordinary 401 rather than a distinct "session too old"
 * response.
 *
 * <p>Does nothing when there is no existing session ({@code getSession(false)})
 * or the session has not outlived the policy: no session is created here, and
 * an unexpired one is left completely alone, idle-timeout renewal included.
 */
public class AbsoluteSessionLifetimeFilter extends OncePerRequestFilter {

    private final AbsoluteSessionLifetimePolicy policy;
    private final Clock clock;

    public AbsoluteSessionLifetimeFilter(AbsoluteSessionLifetimePolicy policy, Clock clock) {
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Instant createdAt = Instant.ofEpochMilli(session.getCreationTime());
            if (policy.isExpired(createdAt, clock.instant())) {
                session.invalidate();
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
