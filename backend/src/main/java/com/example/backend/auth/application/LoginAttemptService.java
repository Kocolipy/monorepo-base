package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.domain.AccountSessions;
import com.example.backend.scim.domain.LockoutPolicy;
import com.example.backend.scim.domain.NormalizedUserName;
import com.example.backend.scim.domain.ScimLoginState;
import com.example.backend.scim.domain.ScimUser;
import com.example.backend.scim.domain.ScimUserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records how each login attempt ended, so repeated failures lock an identity and an accepted login
 * clears the run.
 *
 * <p>Its one caller is {@link LoginService}, which records every attempt it makes; nothing else
 * counts attempts. The counting is explicit rather than driven by Spring Security's authentication
 * events — see {@code /docs/adr/0001-count-login-attempts-on-the-login-path.md}. Enforcement is not
 * here: {@link LoginIdentityService} reports a locked identity to Spring Security, which rejects it
 * before any password is checked.
 *
 * <p>Each method is its own transaction, and both write through the SCIM User port's narrow
 * login-state operation. Narrow matters twice over. It cannot revert a profile attribute a connector
 * or an administrator changed between this attempt starting and finishing — and it does not advance
 * the resource's version, because a failure run is not a SCIM attribute: a wrong password changes
 * nothing a connector can read, and moving the ETag would invalidate every cached copy of the User
 * on every mistyped password.
 *
 * <p>Imposing a lock also ends the identity's live sessions, because a lock that left them alone
 * would close the front door while the identity kept acting through a session it already held. The
 * revocation runs after the transaction commits, for the reason
 * {@link IdentityAdministrationService#deactivate} defers its own: Redis is not in the transaction,
 * and a revocation already performed cannot be undone by a rollback — see
 * {@code /docs/adr/0002-revoke-sessions-after-commit.md}.
 *
 * <p>This is also where the login path's audit events are recorded, for the same reason the counting
 * is here: this class already holds the identity before and after the transition, so it can tell a
 * lockout being imposed from one already in force without a second read or a second guess.
 */
@Service
public class LoginAttemptService {

    private final ScimUserRepository users;
    private final AccountSessions sessions;
    private final AfterCommit afterCommit;
    private final LockoutPolicy policy;
    private final AuditTrail audit;
    private final Clock clock;

    public LoginAttemptService(
            ScimUserRepository users,
            AccountSessions sessions,
            AfterCommit afterCommit,
            LockoutPolicy policy,
            AuditTrail audit,
            Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.afterCommit = afterCommit;
        this.policy = policy;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Counts a rejected attempt against {@code username}, locking the identity once the policy's
     * limit is reached — and never locking the Bootstrap Admin, whose failures are counted and
     * audited like anyone's but cannot close the deployment's last way in.
     *
     * <p>The exemption is read off the User's own reservation marker rather than by comparing its
     * name to a configured string. That is the substantive change from the account aggregate's
     * version of this method: the identity that must never lock is recognised by what it IS, so a
     * rename cannot move the exemption and a second identity cannot acquire it by taking the
     * configured name.
     *
     * <p>An unknown username is ignored rather than recorded. Nothing is created for it, so a caller
     * cannot learn from timing or from stored state whether the name exists. It still produces a
     * {@code LOGIN_FAILURE} event — with no subject, because there is no identity to name and the
     * submitted username is the one thing that must not be recorded in its place. An administrator
     * reading a run of subject-less failures is seeing attempts against names that do not exist,
     * which is exactly the distinction worth having.
     *
     * <p>Every append on this path is fail-open: the caller is already receiving a bare {@code 401}
     * and a trail that cannot be written must not change that answer. See {@link AuditTrail}.
     */
    @Transactional
    public void recordFailure(String username, AuditRefusalReason reason) {
        Instant now = clock.instant();
        Optional<ScimUser> found = find(username);
        if (found.isEmpty()) {
            audit.recordLoginFailure(null, AuditRefusalReason.UNKNOWN_ACCOUNT);
            return;
        }

        ScimUser user = found.get();
        ScimLoginState before = user.login();
        ScimLoginState after = user.isExemptFromLockout()
                ? before.withFailureCounted()
                : before.withFailureRecorded(policy, now);
        users.updateLoginState(user.id(), after);
        if (after.isLocked() && !before.isLocked()) {
            audit.recordLockoutSet(user.id());
            afterCommit.run(() -> sessions.revokeAll(user.id()));
        }
        audit.recordLoginFailure(user.id(), reason);
    }

    /**
     * Clears the failure run of an identity that has just logged in.
     *
     * <p>The success event is fail-closed, unlike everything on the failure path: a session this
     * service could not account for is one it does not issue. The append joins this transaction, so
     * it takes the cleared failure run down with it if it cannot be written.
     */
    @Transactional
    public void recordSuccess(String username) {
        find(username).ifPresent(user -> {
            ScimLoginState cleared = user.login().withFailureRunCleared();
            if (cleared != user.login()) {
                users.updateLoginState(user.id(), cleared);
            }
            audit.recordLoginSuccess(user.id());
        });
    }

    /**
     * The identity behind a submitted name, looked up on the normalized form uniqueness is decided
     * on.
     *
     * <p>A blank submission is nobody rather than an error: it reaches here only if the web adapter's
     * validation was bypassed, and the honest answer is the same one an unknown name gets.
     */
    private Optional<ScimUser> find(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return users.findByNormalizedUserName(NormalizedUserName.of(username));
    }
}
