package com.example.backend.auth.application;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Owns account seeding and translates persisted accounts for Spring Security. */
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
     * <p>One exception: an account that exists but carries no email or no
     * creation timestamp predates those columns, and the administrative listing
     * has nothing to report for it. Those two fields alone are backfilled from
     * configuration, so the listing is complete after one restart.
     */
    public void seedDefaults(AccountSeed user, AccountSeed admin) {
        seed(user, AccountRole.USER);
        seed(admin, AccountRole.ADMIN);
    }

    /** Every account, for administrative review. Never carries a password hash. */
    public List<AccountSummary> listAccounts() {
        return accounts.findAllOrderedByUsername().stream()
                .map(account -> new AccountSummary(
                        account.username(),
                        account.email(),
                        account.role(),
                        account.enabled(),
                        account.createdAt()))
                .toList();
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
                    seed.email(),
                    true,
                    clock.instant()));
            return;
        }

        Account account = existing.get();
        Account backfilled = account.withProfileBackfilled(seed.email(), clock.instant());
        if (backfilled != account) {
            accounts.save(backfilled);
        }
    }

    /** One configured startup account. The role is this service's to assign. */
    public record AccountSeed(String username, String password, String email) {
    }
}
