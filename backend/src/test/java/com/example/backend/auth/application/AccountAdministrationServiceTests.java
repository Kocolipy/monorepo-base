package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountAdministrationServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private static final Duration LOCKOUT = Duration.ofMinutes(5);

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);

    private AccountAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new AccountAdministrationService(accounts, clock);
    }

    // Reviewing who has access

    @Test
    void listsEveryAccountWithoutItsPasswordHash() {
        accounts.save(account("ada", AccountRole.ADMIN));
        accounts.save(account("bob", AccountRole.USER));

        assertThat(service.listAccounts()).containsExactly(
                new AccountSummary("ada", AccountRole.ADMIN, true, false, null, NOW),
                new AccountSummary("bob", AccountRole.USER, true, false, null, NOW));
    }

    @Test
    void listsAccountsOrderedByUsername() {
        accounts.save(account("zoe", AccountRole.USER));
        accounts.save(account("ada", AccountRole.ADMIN));
        accounts.save(account("bob", AccountRole.USER));

        assertThat(service.listAccounts())
                .extracting(AccountSummary::username)
                .containsExactly("ada", "bob", "zoe");
    }

    @Test
    void listsNoAccountsWhenNoneAreStored() {
        assertThat(service.listAccounts()).isEmpty();
    }

    /**
     * Whether a lockout is in force is the server's answer, not a comparison the
     * client makes: only the server's clock is the one the login path enforces on.
     */
    @Test
    void reportsALockoutAsInForceUntilItExpires() {
        accounts.save(locked("ada"));

        assertThat(service.listAccounts()).first()
                .extracting(AccountSummary::locked, AccountSummary::lockedUntil)
                .containsExactly(true, NOW.plus(LOCKOUT));

        clock.advanceBy(LOCKOUT);

        assertThat(service.listAccounts()).first()
                .extracting(AccountSummary::locked, AccountSummary::lockedUntil)
                .containsExactly(false, NOW.plus(LOCKOUT));
    }

    // Disabling

    @Test
    void disablingClosesTheAccountAndReportsItBack() {
        accounts.save(account("bob", AccountRole.USER));

        AccountSummary disabled = service.disable("bob", "ada");

        assertThat(disabled.enabled()).isFalse();
        assertThat(accounts.require("bob").enabled()).isFalse();
    }

    /**
     * The two capabilities are separate, so disabling must not double as a
     * penalty reset: the failure run is evidence, and it is most wanted at exactly
     * the moment an account is being closed.
     */
    @Test
    void disablingLeavesTheFailureRunAndLockoutUntouched() {
        accounts.save(locked("bob"));

        service.disable("bob", "ada");

        Account stored = accounts.require("bob");
        assertThat(stored.failedLoginAttempts()).isEqualTo(3);
        assertThat(stored.lockedUntil()).isEqualTo(NOW.plus(LOCKOUT));
        assertThat(stored.isLocked(NOW)).isTrue();
    }

    @Test
    void disablingAnAlreadyDisabledAccountWritesNothing() {
        accounts.save(account("bob", AccountRole.USER).withEnabled(false));
        int before = accounts.saves();

        assertThat(service.disable("bob", "ada").enabled()).isFalse();
        assertThat(accounts.saves()).isEqualTo(before);
    }

    @Test
    void refusesToDisableTheAccountMakingTheRequest() {
        accounts.save(account("ada", AccountRole.ADMIN));
        accounts.save(account("zoe", AccountRole.ADMIN));

        assertThatThrownBy(() -> service.disable("ada", "ada"))
                .isInstanceOf(UnsafeAccountChangeException.class)
                .hasMessage("An account cannot disable itself");
        assertThat(accounts.require("ada").enabled()).isTrue();
    }

    /**
     * Nothing else could undo this: enabling an account needs an administrator who
     * can log in, and the last one disabled cannot.
     */
    @Test
    void refusesToDisableTheLastEnabledAdministrator() {
        accounts.save(account("ada", AccountRole.ADMIN));
        accounts.save(account("bob", AccountRole.USER));

        assertThatThrownBy(() -> service.disable("ada", "zoe"))
                .isInstanceOf(UnsafeAccountChangeException.class)
                .hasMessageContaining("last enabled administrator");
        assertThat(accounts.require("ada").enabled()).isTrue();
    }

    @Test
    void allowsDisablingAnAdministratorWhileAnotherEnabledOneRemains() {
        accounts.save(account("ada", AccountRole.ADMIN));
        accounts.save(account("zoe", AccountRole.ADMIN));

        assertThat(service.disable("ada", "zoe").enabled()).isFalse();
    }

    /**
     * A locked administrator still counts as a means of recovery: the lockout ends
     * by itself, where a disabled account waits for someone with the rights to
     * reopen it.
     */
    @Test
    void countsALockedAdministratorAsAvailableForRecovery() {
        accounts.save(locked("ada", AccountRole.ADMIN));
        accounts.save(account("zoe", AccountRole.ADMIN));

        assertThat(service.disable("zoe", "ada").enabled()).isFalse();
    }

    @Test
    void doesNotCountADisabledAdministratorAsAvailableForRecovery() {
        accounts.save(account("ada", AccountRole.ADMIN));
        accounts.save(account("zoe", AccountRole.ADMIN).withEnabled(false));

        assertThatThrownBy(() -> service.disable("ada", "bob"))
                .isInstanceOf(UnsafeAccountChangeException.class);
    }

    // Enabling

    @Test
    void enablingReopensTheAccount() {
        accounts.save(account("bob", AccountRole.USER).withEnabled(false));

        assertThat(service.enable("bob").enabled()).isTrue();
        assertThat(accounts.require("bob").enabled()).isTrue();
    }

    /**
     * The decision the user asked for: restoring access is not a finding that the
     * failed logins did not happen, so the lockout survives and needs its own
     * call.
     */
    @Test
    void enablingDoesNotLiftALockout() {
        accounts.save(locked("bob").withEnabled(false));

        AccountSummary enabled = service.enable("bob");

        assertThat(enabled.enabled()).isTrue();
        assertThat(enabled.locked()).isTrue();
        assertThat(accounts.require("bob").failedLoginAttempts()).isEqualTo(3);
    }

    @Test
    void enablingAnAlreadyEnabledAccountWritesNothing() {
        accounts.save(account("bob", AccountRole.USER));
        int before = accounts.saves();

        assertThat(service.enable("bob").enabled()).isTrue();
        assertThat(accounts.saves()).isEqualTo(before);
    }

    // Unlocking

    @Test
    void unlockingEndsTheLockoutAndTheFailureRun() {
        accounts.save(locked("bob"));

        AccountSummary unlocked = service.unlock("bob");

        assertThat(unlocked.locked()).isFalse();
        assertThat(unlocked.lockedUntil()).isNull();
        assertThat(accounts.require("bob").failedLoginAttempts()).isZero();
    }

    /** The converse of the decision above: unlocking is not a reinstatement. */
    @Test
    void unlockingDoesNotEnableADisabledAccount() {
        accounts.save(locked("bob").withEnabled(false));

        AccountSummary unlocked = service.unlock("bob");

        assertThat(unlocked.locked()).isFalse();
        assertThat(unlocked.enabled()).isFalse();
        assertThat(accounts.require("bob").enabled()).isFalse();
    }

    @Test
    void unlockingAnAccountThatIsNotLockedWritesNothing() {
        accounts.save(account("bob", AccountRole.USER));
        int before = accounts.saves();

        assertThat(service.unlock("bob").locked()).isFalse();
        assertThat(accounts.saves()).isEqualTo(before);
    }

    /**
     * An expired lockout leaves its instant behind, so "not locked" is not the
     * same as "nothing to clear" — unlocking such an account still tidies the run
     * that would otherwise carry into the next failure.
     */
    @Test
    void unlockingClearsAnExpiredLockoutThatIsStillRecorded() {
        accounts.save(locked("bob"));
        clock.advanceBy(LOCKOUT);

        service.unlock("bob");

        assertThat(accounts.require("bob").lockedUntil()).isNull();
        assertThat(accounts.require("bob").failedLoginAttempts()).isZero();
    }

    // Unknown accounts

    @Test
    void refusesToActOnAnAccountThatDoesNotExist() {
        assertThatThrownBy(() -> service.disable("nobody", "ada"))
                .isInstanceOf(UnknownAccountException.class)
                .hasMessage("No account named nobody");
        assertThatThrownBy(() -> service.enable("nobody"))
                .isInstanceOf(UnknownAccountException.class);
        assertThatThrownBy(() -> service.unlock("nobody"))
                .isInstanceOf(UnknownAccountException.class);
        assertThat(accounts.findByUsername("nobody")).isEmpty();
    }

    private static Account account(String username, AccountRole role) {
        return new Account(username, "hash", role, 0, null, true, NOW);
    }

    private static Account locked(String username) {
        return locked(username, AccountRole.USER);
    }

    private static Account locked(String username, AccountRole role) {
        return new Account(username, "hash", role, 3, NOW.plus(LOCKOUT), true, NOW);
    }
}
