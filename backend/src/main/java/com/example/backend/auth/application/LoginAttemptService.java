package com.example.backend.auth.application;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Clock;
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
 */
@Service
public class LoginAttemptService {

    private final AccountRepository accounts;
    private final LockoutPolicy policy;
    private final Clock clock;

    public LoginAttemptService(AccountRepository accounts, LockoutPolicy policy, Clock clock) {
        this.accounts = accounts;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * Counts a rejected attempt against {@code username}, locking the account
     * once the policy's limit is reached.
     *
     * <p>An unknown username is ignored rather than recorded. Nothing is created
     * for it, so a caller cannot learn from timing or from stored state whether
     * the name exists.
     */
    @Transactional
    public void recordFailure(String username) {
        accounts.findByUsername(username)
                .map(account -> account.withFailureRecorded(policy, clock.instant()))
                .ifPresent(accounts::save);
    }

    /** Clears the failure run of an account that has just logged in. */
    @Transactional
    public void recordSuccess(String username) {
        accounts.findByUsername(username).ifPresent(account -> {
            Account cleared = account.withSuccessfulLogin();
            if (cleared != account) {
                accounts.save(cleared);
            }
        });
    }
}
