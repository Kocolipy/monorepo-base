package com.example.backend.auth.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.AccountSessions;
import com.example.backend.observability.LogEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The account use cases an administrator drives: reviewing who has access, and
 * changing whether an account may be used.
 *
 * <p>Separate from {@link AccountService}, which serves the login path — seeding
 * and reporting an account to Spring Security. Keeping them apart means the
 * authentication path does not depend on a class that also mutates accounts, and
 * the administrative guards below live beside nothing that the login path could
 * accidentally bypass.
 *
 * <p>Enabling and unlocking are separate operations here as well as in the
 * domain. Either one alone leaves the other in force, so restoring a suspended
 * account that also locked itself out takes both calls — deliberately, since an
 * administrator should say which of the two they mean.
 */
@Service
public class AccountAdministrationService {

    private static final Logger log = LoggerFactory.getLogger(AccountAdministrationService.class);

    private static final String DISABLE_ACTION = "account.disable";
    private static final String ENABLE_ACTION = "account.enable";
    private static final String UNLOCK_ACTION = "account.unlock";

    private final AccountRepository accounts;
    private final AccountSessions sessions;
    private final AfterCommit afterCommit;
    private final AuditTrail audit;
    private final Clock clock;

    public AccountAdministrationService(
            AccountRepository accounts,
            AccountSessions sessions,
            AfterCommit afterCommit,
            AuditTrail audit,
            Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.afterCommit = afterCommit;
        this.audit = audit;
        this.clock = clock;
    }

    /** Every account, for administrative review. Never carries a password hash. */
    public List<AccountSummary> listAccounts() {
        Instant now = clock.instant();
        return accounts.findAllOrderedByUsername().stream()
                .map(account -> summarize(account, now))
                .toList();
    }

    /**
     * Closes an account to logins and ends the sessions it is already holding, so
     * it stops acting now rather than when those sessions expire. The lockout is
     * untouched: this is not a penalty and says nothing about the failure run.
     *
     * <p>Two refusals guard against an administrator removing the only means of
     * reversing this. Neither is about authorization — the caller is an admin, and
     * the action is what is refused. A refused disable revokes nothing: both
     * checks run before anything is written or ended.
     *
     * <p>The revocation happens after the transaction commits, so an account
     * whose row could not be written keeps its sessions — and so does one whose
     * write was rolled back after this method returned, which is the reason it is
     * not simply the last statement here: Redis is not in the transaction, and a
     * revocation already performed cannot be undone by a rollback. A refused or
     * rolled-back disable therefore revokes nothing, and the listing and the
     * sessions never disagree.
     *
     * <p>It is still not a lock. A login already in flight reads {@code enabled}
     * as it was before this transaction committed, and a session it mints is
     * revoked only if it commits before the revocation runs; deferring to after
     * the commit bounds that window at the commit rather than straddling it, which
     * is as narrow as it gets without holding a lock on the account.
     *
     * <p>The cost of the ordering: if the revocation itself fails, the account is
     * durably disabled while its sessions survive, and the failure surfaces to the
     * caller. Repeating the disable is how an administrator acts on that — it
     * writes nothing and revokes again.
     */
    @Transactional
    public AccountSummary disable(String username, String requestedBy) {
        Account account = require(username);
        if (account.username().equals(requestedBy)) {
            throw refuse(DISABLE_ACTION, "SelfDisable", "An account cannot disable itself");
        }
        if (isLastEnabledAdministrator(account)) {
            throw refuse(
                    DISABLE_ACTION,
                    "LastEnabledAdministrator",
                    "Disabling the last enabled administrator would leave nobody able to"
                            + " enable it again");
        }
        AccountSummary disabled = applyEnabled(account, false);
        audit.recordAccountDisabled(actorId(requestedBy), account.id());
        afterCommit.run(() -> sessions.revokeAll(account.id()));
        succeeded(DISABLE_ACTION);
        return disabled;
    }

    /**
     * Reopens an account to logins. A lockout it is serving is left standing: the
     * penalty either expires on its own or is lifted by {@link #unlock}, and
     * restoring access is not a finding that the failed logins did not happen.
     *
     * <p>Sessions are not given back. {@link #disable} ended them, and a session
     * is not a thing an administrator can hand over — the account signs in again.
     */
    @Transactional
    public AccountSummary enable(String username, String requestedBy) {
        Account account = require(username);
        AccountSummary enabled = applyEnabled(account, true);
        audit.recordAccountEnabled(actorId(requestedBy), account.id());
        succeeded(ENABLE_ACTION);
        return enabled;
    }

    /**
     * Ends a lockout early, clearing the failure run with it. Says nothing about
     * whether the account is enabled — a disabled account can be unlocked, and
     * stays disabled.
     *
     * <p>Idempotent: an account serving no lockout is returned unchanged and
     * nothing is written.
     */
    @Transactional
    public AccountSummary unlock(String username, String requestedBy) {
        Account account = require(username);
        Account unlocked = account.withLockoutCleared();
        if (unlocked != account) {
            accounts.updateLockout(unlocked);
        }
        audit.recordLockoutLiftedByUnlock(actorId(requestedBy), account.id());
        succeeded(UNLOCK_ACTION);
        return summarize(unlocked, clock.instant());
    }

    /**
     * The stable id behind the administrator's username, for the event's actor
     * reference.
     *
     * <p>{@code null} when the name resolves to no account, which is not a case
     * worth refusing the operation over: the caller is an authenticated
     * administrator whose own row could have been renamed between authentication
     * and this call, and an event recorded with no actor is more use than no event
     * at all. What it never becomes is the username itself.
     */
    private UUID actorId(String requestedBy) {
        return accounts.findByUsername(requestedBy).map(Account::id).orElse(null);
    }

    private AccountSummary applyEnabled(Account account, boolean shouldBeEnabled) {
        Account updated = account.withEnabled(shouldBeEnabled);
        if (updated != account) {
            accounts.updateEnabled(updated);
        }
        return summarize(updated, clock.instant());
    }

    /**
     * Whether this account is the only administrator that could still perform
     * administrative work. A disabled admin cannot log in, so it does not count;
     * a locked one is only temporarily out and does.
     */
    private boolean isLastEnabledAdministrator(Account account) {
        if (account.role() != AccountRole.ADMIN || !account.enabled()) {
            return false;
        }
        return accounts.findAllOrderedByUsername().stream()
                .filter(other -> other.role() == AccountRole.ADMIN && other.enabled())
                .allMatch(other -> other.username().equals(account.username()));
    }

    private Account require(String username) {
        return accounts.findByUsername(username)
                .orElseThrow(() -> new UnknownAccountException(username));
    }

    /**
     * Records an administrative write that went through.
     *
     * <p>The record names the action and nothing else. It deliberately identifies
     * neither the account acted on nor the administrator who acted: both are
     * currently identified by {@code username} only, and a {@code userName} is not
     * something this service writes to a log. Naming who changed what is the audit
     * trail's responsibility rather than this stream's, and becomes possible here
     * — as {@code scim.resource.id} in the logging context — once the account
     * aggregate carries a stable id. Until then a log line says an administrative
     * change happened and when, which is what an operator watching for unexpected
     * activity needs, and the API response says which account to the caller who is
     * entitled to know.
     */
    private static void succeeded(String action) {
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, action)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .log("Administrative account change applied");
    }

    /**
     * Records a refused administrative write and returns the exception to throw, so
     * the refusal cannot be logged without being raised or raised without being
     * logged. {@code reason} is a fixed label from this class, never the message —
     * a message is written for a human and is free to grow a value in it later.
     */
    private static UnsafeAccountChangeException refuse(
            String action, String reason, String message) {
        log.atWarn()
                .addKeyValue(LogEvent.ACTION, action)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.FAILURE)
                .addKeyValue(LogEvent.REASON, reason)
                .log("Administrative account change refused");
        return new UnsafeAccountChangeException(message);
    }

    private static AccountSummary summarize(Account account, Instant now) {
        return new AccountSummary(
                account.username(),
                account.role(),
                account.enabled(),
                account.isLocked(now),
                account.lockedUntil(),
                account.createdAt());
    }
}
