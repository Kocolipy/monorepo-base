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

    /**
     * A fixed passphrase encoded with the same {@link PasswordEncoder} this
     * service is configured with, standing in for a credentialless account's
     * absent hash. Computed once, on first use, from whatever encoder is
     * injected — mirroring how {@code DaoAuthenticationProvider} builds its own
     * dummy hash for an unknown username — rather than a literal encoded string
     * fixed at compile time, which would silently stop matching the encoder's
     * parameters the moment they changed.
     *
     * <p>No password verifies against it — the encoded value matches nothing a
     * caller can submit — so {@code DaoAuthenticationProvider} still runs one
     * real Argon2id comparison, at the same cost as a genuine hash, before
     * refusing. That uniformity, not the string's content, is why one is needed
     * at all: passing {@code null} through to
     * {@code User.withUsername(...).password(...)} would throw before any
     * comparison happened, which is a different and distinguishable failure
     * mode from a wrong password.
     */
    private volatile String noPasswordSetMarker;

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
     *
     * <p>A credentialless account — one with no password hash set — reports
     * {@link #noPasswordSetMarker()} rather than {@code null}: the account
     * exists and may be enabled and unlocked, but nothing submitted can match a
     * hash nobody wrote, so it is refused on the password check like any other
     * wrong password, in the same amount of work.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        Account account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return User.withUsername(account.username())
                .password(account.passwordHash() == null
                        ? noPasswordSetMarker()
                        : account.passwordHash())
                .roles(account.role().name())
                .accountLocked(account.isLocked(clock.instant()))
                // The listing reports this flag, so authentication has to honour
                // it: an `enabled: false` row a disabled account could still log
                // in with would make the listing a lie.
                .disabled(!account.enabled())
                .build();
    }

    /**
     * Lazily computed and cached: encoding is the expensive Argon2id step this
     * marker exists to force on the refusal path, so it must happen once per
     * process, not on every credentialless login attempt.
     */
    private String noPasswordSetMarker() {
        String cached = noPasswordSetMarker;
        if (cached == null) {
            synchronized (this) {
                cached = noPasswordSetMarker;
                if (cached == null) {
                    cached = passwordEncoder.encode("no-password-set");
                    noPasswordSetMarker = cached;
                }
            }
        }
        return cached;
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
