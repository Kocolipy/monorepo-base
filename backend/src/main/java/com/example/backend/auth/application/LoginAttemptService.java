package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
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
 * <p>This is also where the login path's audit events are recorded, for the same
 * reason the counting is here: this class already holds the account before and
 * after the transition, so it can tell a lockout being imposed from one already in
 * force, and an expired lockout from an absent one, without a second read or a
 * second guess. Recording them at the call site would mean re-deriving state that
 * was only available here.
 */
@Service
public class LoginAttemptService {

    private final AccountRepository accounts;
    private final LockoutPolicy policy;
    private final AuditTrail audit;
    private final Clock clock;

    public LoginAttemptService(
            AccountRepository accounts,
            LockoutPolicy policy,
            AuditTrail audit,
            Clock clock) {
        this.accounts = accounts;
        this.policy = policy;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Counts a rejected attempt against {@code username}, locking the account
     * once the policy's limit is reached.
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
        recordAnyExpiredLockout(account, now);

        Account updated = account.withFailureRecorded(policy, now);
        accounts.save(updated);
        if (updated.isLocked(now) && !account.isLocked(now)) {
            audit.recordLockoutSet(account.id());
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
            recordAnyExpiredLockout(account, clock.instant());
            Account cleared = account.withSuccessfulLogin();
            if (cleared != account) {
                accounts.save(cleared);
            }
            audit.recordLoginSuccess(account.id());
        });
    }

    /**
     * Records a lockout that has run out, if this account was serving one.
     *
     * <p>An expiry is the one audited transition nobody performs, so there is no
     * request to record it under and no moment it obviously belongs to. This is
     * that moment: a login attempt against an account whose {@code lockedUntil} is
     * recorded but past is the first time the service acts on the expiry — the next
     * transition either starts a fresh failure run or clears the field outright, so
     * after this call the evidence that a lockout existed is gone from the row.
     * Recorded before the transition for exactly that reason.
     */
    private void recordAnyExpiredLockout(Account account, Instant now) {
        if (account.lockedUntil() != null && !account.isLocked(now)) {
            audit.recordLockoutLiftedByExpiry(account.id());
        }
    }
}
