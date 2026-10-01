package com.example.backend.auth.config;

import com.example.backend.observability.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts the authenticated caller's stable id in the logging context as
 * {@code user.id} for the rest of the request, so every record the request emits
 * says whose request it was.
 *
 * <p>The id is the one the login wrote into the session's principal index — the
 * SCIM resource id, which survives a later userName change and names nobody to a
 * reader without database access. It is never the {@code Authentication}'s name,
 * which is the userName and is exactly what must not be logged.
 *
 * <p>Placed after the security context is loaded from the session (see
 * {@link SecurityConfig#securityFilterChain}), and it requires both halves: an
 * authenticated context AND an index holding a well-formed id. A session that
 * carries only one of them — or an index value that is not a UUID — puts nothing
 * in the context, which is the same answer an anonymous request gets.
 */
public class SessionUserLogContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // A null id leaves user.id absent for the chain, the same answer an anonymous
        // request gets; one scope covers both cases.
        try (LogContext.Scope scope = LogContext.userId(sessionUserId(request))) {
            chain.doFilter(request, response);
        }
    }

    private static UUID sessionUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null
                || !(session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME)
                        instanceof String indexed)) {
            return null;
        }
        try {
            return UUID.fromString(indexed);
        } catch (IllegalArgumentException notAnId) {
            return null;
        }
    }
}
