package com.example.backend.auth.application;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.time.Clock;
import java.util.Optional;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Serves the login path: startup seeding, and reporting an account to Spring
 * Security. Administrative review and changes live in
 * {@link AccountAdministrationService}, so nothing the login path depends on is
 * also able to mutate an account.
 */
@Service
public class AccountService implements UserDetailsService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountService(
            AccountRepository accounts, PasswordEncoder passwordEncoder, Clock clock) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Creates the two configured accounts if they are absent, and never
     * overwrites one that exists — its password, role and login history are
     * whatever the operator and the login path made them.
     *
     * <p>One exception: an account that exists but carries no creation timestamp
     * predates that column, and the administrative listing has nothing to report
     * for it. That one field alone is backfilled, so the listing is complete
     * after one restart.
     */
    public void seedDefaults(AccountSeed user, AccountSeed admin) {
        seed(user, AccountRole.USER);
        seed(admin, AccountRole.ADMIN);
    }

    /**
     * Reports the account to Spring Security, including whether it is currently
     * locked and whether an administrator has disabled it. Carrying both here is
     * what rejects such an account with its correct password:
     * {@code DaoAuthenticationProvider} checks account status before it checks
     * the password, so the credentials are never even compared.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        Account account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .roles(account.role().name())
                .accountLocked(account.isLocked(clock.instant()))
                // The listing reports this flag, so authentication has to honour
                // it: an `enabled: false` row a disabled account could still log
                // in with would make the listing a lie.
                .disabled(!account.enabled())
                .build();
    }

    private void seed(AccountSeed seed, AccountRole role) {
        Optional<Account> existing = accounts.findByUsername(seed.username());
        if (existing.isEmpty()) {
            accounts.save(new Account(
                    seed.username(),
                    passwordEncoder.encode(seed.password()),
                    role,
                    0,
                    null,
                    true,
                    clock.instant()));
            return;
        }

        Account account = existing.get();
        Account backfilled = account.withCreatedAtBackfilled(clock.instant());
        if (backfilled != account) {
            accounts.save(backfilled);
        }
    }

    /** One configured startup account. The role is this service's to assign. */
    public record AccountSeed(String username, String password) {
    }
}
