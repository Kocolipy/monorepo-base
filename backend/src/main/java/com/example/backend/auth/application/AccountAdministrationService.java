package com.example.backend.auth.application;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
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

    private final AccountRepository accounts;
    private final Clock clock;

    public AccountAdministrationService(AccountRepository accounts, Clock clock) {
        this.accounts = accounts;
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
     * Closes an account to logins until someone enables it again. The lockout is
     * untouched: this is not a penalty and says nothing about the failure run.
     *
     * <p>Two refusals guard against an administrator removing the only means of
     * reversing this. Neither is about authorization — the caller is an admin, and
     * the action is what is refused.
     *
     * <p>An existing session is NOT ended by this. Spring Security evaluates
     * account status when authenticating, and later requests read their
     * authentication back out of the session, so a disabled account keeps working
     * until its session expires. Revoking those needs a session repository that
     * can be searched by principal.
     */
    @Transactional
    public AccountSummary disable(String username, String requestedBy) {
        Account account = require(username);
        if (account.username().equals(requestedBy)) {
            throw new UnsafeAccountChangeException("An account cannot disable itself");
        }
        if (isLastEnabledAdministrator(account)) {
            throw new UnsafeAccountChangeException(
                    "Disabling the last enabled administrator would leave nobody able to"
                            + " enable it again");
        }
        return applyEnabled(account, false);
    }

    /**
     * Reopens an account to logins. A lockout it is serving is left standing: the
     * penalty either expires on its own or is lifted by {@link #unlock}, and
     * restoring access is not a finding that the failed logins did not happen.
     */
    @Transactional
    public AccountSummary enable(String username) {
        return applyEnabled(require(username), true);
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
    public AccountSummary unlock(String username) {
        Account account = require(username);
        Account unlocked = account.withLockoutCleared();
        if (unlocked != account) {
            accounts.updateLockout(unlocked);
        }
        return summarize(unlocked, clock.instant());
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

    private static AccountSummary summarize(Account account, Instant now) {
        return new AccountSummary(
                account.username(),
                account.email(),
                account.role(),
                account.enabled(),
                account.isLocked(now),
                account.lockedUntil(),
                account.createdAt());
    }
}
