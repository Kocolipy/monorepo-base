package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountSessions;
import com.example.backend.auth.domain.BootstrapAdmin;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records how each login attempt ended, so repeated failures lock an account and
 * an accepted login clears the run.
 *
 * <p>Its one caller is {@link LoginService}, which records every attempt it
 * makes; nothing else counts attempts. The counting is explicit rather than
 * driven by Spring Security's authentication events — see
 * {@code /docs/adr/0001-count-login-attempts-on-the-login-path.md}. Enforcement
 * is not here: {@link AccountService} reports a locked account to Spring
 * Security, which rejects it before any password is checked.
 *
 * <p>Each method is its own transaction, and both write the account whole from a
 * row read inside it, which is what the narrow administrative writes on
 * {@link com.example.backend.auth.domain.AccountRepository} exist to avoid
 * racing.
 *
 * <p>Imposing a lock also ends the account's live sessions, because a lock that
 * left them alone would close the front door while the account kept acting
 * through a session it already held. The revocation runs after the transaction
 * commits, for the reason {@link AccountAdministrationService#disable} defers its
 * own: Redis is not in the transaction, and a revocation already performed cannot
 * be undone by a rollback — see
 * {@code /docs/adr/0002-revoke-sessions-after-commit.md}.
 *
 * <p>This is also where the login path's audit events are recorded, for the same
 * reason the counting is here: this class already holds the account before and
 * after the transition, so it can tell a lockout being imposed from one already in
 * force without a second read or a second guess. Recording them at the call site
 * would mean re-deriving state that was only available here.
 */
@Service
public class LoginAttemptService {

    private final AccountRepository accounts;
    private final AccountSessions sessions;
    private final AfterCommit afterCommit;
    private final LockoutPolicy policy;
    private final BootstrapAdmin bootstrapAdmin;
    private final AuditTrail audit;
    private final Clock clock;

    public LoginAttemptService(
            AccountRepository accounts,
            AccountSessions sessions,
            AfterCommit afterCommit,
            LockoutPolicy policy,
            BootstrapAdmin bootstrapAdmin,
            AuditTrail audit,
            Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.afterCommit = afterCommit;
        this.policy = policy;
        this.bootstrapAdmin = bootstrapAdmin;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Counts a rejected attempt against {@code username}, locking the account
     * once the policy's limit is reached — and never locking the Bootstrap Admin,
     * whose failures are counted and audited like any other but cannot close the
     * deployment's last way in. See
     * {@link com.example.backend.auth.domain.BootstrapAdmin}.
     *
     * <p>An unknown username is ignored rather than recorded. Nothing is created
     * for it, so a caller cannot learn from timing or from stored state whether
     * the name exists. It still produces a {@code LOGIN_FAILURE} event — with no
     * subject, because there is no account to name and the submitted username is
     * the one thing that must not be recorded in its place. An administrator
     * reading a run of subject-less failures is seeing attempts against names that
     * do not exist, which is exactly the distinction worth having.
     *
     * <p>Every append on this path is fail-open: the caller is already receiving a
     * bare {@code 401} and a trail that cannot be written must not change that
     * answer. See {@link AuditTrail}.
     */
    @Transactional
    public void recordFailure(String username, AuditRefusalReason reason) {
        Instant now = clock.instant();
        Optional<Account> found = accounts.findByUsername(username);
        if (found.isEmpty()) {
            audit.recordLoginFailure(null, AuditRefusalReason.UNKNOWN_ACCOUNT);
            return;
        }

        Account account = found.get();
        Account updated = bootstrapAdmin.identifies(account)
                ? account.withFailureCounted()
                : account.withFailureRecorded(policy, now);
        accounts.save(updated);
        if (updated.isLocked() && !account.isLocked()) {
            audit.recordLockoutSet(account.id());
            afterCommit.run(() -> sessions.revokeAll(account.id()));
        }
        audit.recordLoginFailure(account.id(), reason);
    }

    /**
     * Clears the failure run of an account that has just logged in.
     *
     * <p>The success event is fail-closed, unlike everything on the failure path: a
     * session this service could not account for is one it does not issue. The
     * append joins this transaction, so it takes the cleared failure run down with
     * it if it cannot be written.
     */
    @Transactional
    public void recordSuccess(String username) {
        accounts.findByUsername(username).ifPresent(account -> {
            Account cleared = account.withSuccessfulLogin();
            if (cleared != account) {
                accounts.save(cleared);
            }
            audit.recordLoginSuccess(account.id());
        });
    }
}
