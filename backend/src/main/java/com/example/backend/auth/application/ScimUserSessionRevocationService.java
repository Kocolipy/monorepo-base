package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.audit.domain.AuditUserAttribute;
import com.example.backend.auth.domain.AccountSessions;
import com.example.backend.scim.domain.ScimUserSessions;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Ends a User's sessions after a SCIM write changed something they were issued against — the
 * login surface's side of {@link ScimUserSessions}.
 *
 * <p>Here rather than in the directory slice because both halves it combines are this slice's:
 * {@link AccountSessions} ends the sessions and {@link AfterCommit} defers the ending to the
 * commit, and the dependency between the slices runs from here to the directory, never back. The
 * SCIM write use case names the port and this is what answers it.
 *
 * <p>The revocation runs after the commit, so a write that is refused, stale or rolled back ends
 * nothing — the ordering ADR 0002 settled for deactivation, applied to every security-relevant
 * SCIM change. Its outcome is recorded as its own event once it has run, because that is the
 * first moment the outcome exists: the write's own event was committed with the write and cannot
 * say whether Redis then did its part.
 *
 * <p>A revocation that fails is recorded as a failure and then propagates, so the connector sees
 * an error rather than a success for a change whose sessions survived. The write itself stays
 * durable — that is the safer half to keep, as ADR 0002 argues: the User can no longer
 * authenticate with what was changed, and repeating the write revokes again.
 */
@Service
public class ScimUserSessionRevocationService implements ScimUserSessions {

    private final AccountSessions sessions;
    private final AfterCommit afterCommit;
    private final AuditTrail audit;

    public ScimUserSessionRevocationService(
            AccountSessions sessions, AfterCommit afterCommit, AuditTrail audit) {
        this.sessions = sessions;
        this.afterCommit = afterCommit;
        this.audit = audit;
    }

    @Override
    public void revokeAfterCommit(UUID connectorId, UUID userId, Set<Cause> causes) {
        if (causes.isEmpty()) {
            throw new IllegalArgumentException("a revocation has a cause");
        }
        Set<AuditUserAttribute> paths = paths(causes);
        afterCommit.run(() -> {
            try {
                sessions.revokeAll(userId);
            } catch (RuntimeException revocationFailed) {
                audit.recordUserSessionsRevoked(connectorId, userId, paths, false);
                throw revocationFailed;
            }
            audit.recordUserSessionsRevoked(connectorId, userId, paths, true);
        });
    }

    /**
     * The attribute whose change each cause is, as the event's changed paths. A deletion changed
     * no attribute — the whole User went — so it contributes none; the revocation event is told
     * apart by the {@code SCIM_USER_DELETE} event committed for the same subject before it.
     */
    private static Set<AuditUserAttribute> paths(Set<Cause> causes) {
        Set<AuditUserAttribute> paths = EnumSet.noneOf(AuditUserAttribute.class);
        for (Cause cause : causes) {
            switch (cause) {
                case DEACTIVATED -> paths.add(AuditUserAttribute.ACTIVE);
                case PASSWORD_CHANGED -> paths.add(AuditUserAttribute.PASSWORD);
                case USER_NAME_CHANGED -> paths.add(AuditUserAttribute.USER_NAME);
                case ROLE_REVOKED -> paths.add(AuditUserAttribute.GROUPS);
                case DELETED -> { }
            }
        }
        return paths;
    }
}
