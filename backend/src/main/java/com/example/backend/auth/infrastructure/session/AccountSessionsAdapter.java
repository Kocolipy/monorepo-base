package com.example.backend.auth.infrastructure.session;

import com.example.backend.auth.domain.AccountSessions;
import java.util.Set;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter for {@link AccountSessions}, backed by Spring Session's
 * principal-name index.
 *
 * <p>The index is what makes this possible at all, and it is not free: a plain
 * session store can only be read by id, so the sessions belonging to a username
 * cannot be found. {@code spring.session.redis.repository-type: indexed} is
 * therefore load-bearing configuration rather than a preference — with the
 * default repository this class has no bean to inject and the application does
 * not start, which is the intended failure. It is loud, and it happens at
 * startup, rather than a disable quietly leaving sessions running.
 *
 * <p>The index is populated from the session's Spring Security context, so it
 * only ever names accounts that authenticated through the login path.
 */
@Component
public class AccountSessionsAdapter implements AccountSessions {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public AccountSessionsAdapter(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    @Override
    public int revokeAll(String username) {
        // Copied out of the returned map before deleting: the lookup's result is
        // the repository's own view, and deleting through it while iterating is
        // not something the interface promises to tolerate.
        Set<String> ids = Set.copyOf(sessions.findByPrincipalName(username).keySet());
        ids.forEach(sessions::deleteById);
        return ids.size();
    }
}
