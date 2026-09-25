package com.example.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.auth.InMemoryAccountRepository;
import com.example.backend.auth.MutableClock;
import com.example.backend.auth.config.SecurityConfig;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRole;
import com.example.backend.auth.domain.LockoutPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Structural equivalence of the three refusal categories this ticket names: an
 * unknown username, a credentialless account, and a real account given the
 * wrong password must each drive exactly one real verification against a dummy
 * hash before the refusal — asserted by counting calls into the encoder, not by
 * measuring wall-clock time, which is what the acceptance criteria for this
 * ticket asks for.
 *
 * <p>The unknown-username case is not code this module wrote: it is
 * {@code DaoAuthenticationProvider}'s own built-in
 * {@code prepareTimingAttackProtection}/{@code mitigateAgainstTimingAttack},
 * which this suite exercises through the real {@link SecurityConfig}-built
 * {@code AuthenticationManager} rather than re-implementing — a duplicate
 * dummy-hash path here would drift from the one actually wired in production
 * the first time either changed independently.
 */
class RefusalTimingEquivalenceTests {

    private static final Instant NOW = Instant.parse("2026-09-24T07:00:00Z");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final MutableClock clock = new MutableClock(NOW);

    private CountingPasswordEncoder passwordEncoder;
    private LoginService login;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        passwordEncoder = new CountingPasswordEncoder(config.passwordEncoder());
        accounts.save(new Account(
                "ada", passwordEncoder.encode("correct-password"), AccountRole.USER));
        accounts.save(new Account("nopass", null, AccountRole.USER));
        AccountService users = new AccountService(accounts, passwordEncoder, clock);
        login = new LoginService(
                config.authenticationManager(users, passwordEncoder),
                new LoginAttemptService(accounts, new LockoutPolicy(5, Duration.ofMinutes(20)),
                        clock),
                users);
    }

    @Test
    void aWrongPasswordOnARealAccountRunsExactlyOneVerification() {
        passwordEncoder.matchCalls.set(0);

        refuse("ada", "wrong-password");

        assertThat(passwordEncoder.matchCalls).hasValue(1);
    }

    @Test
    void aCredentiallessAccountRunsExactlyOneVerification() {
        passwordEncoder.matchCalls.set(0);

        refuse("nopass", "anything");

        assertThat(passwordEncoder.matchCalls).hasValue(1);
    }

    @Test
    void anUnknownUsernameRunsExactlyOneVerification() {
        passwordEncoder.matchCalls.set(0);

        refuse("nobody", "anything");

        assertThat(passwordEncoder.matchCalls).hasValue(1);
    }

    /**
     * Not just "one call each" but against a dummy hash of the ticket's required
     * shape: every category compares against an {@code {argon2id}}-prefixed
     * hash, so none of them can be picked out by running a cheaper or
     * differently-shaped comparison. The three dummy hashes are not required to
     * be byte-identical — the unknown-username path is
     * {@code DaoAuthenticationProvider}'s own cached comparand, encoded once
     * from a different fixed passphrase than the credentialless path's — only
     * that each is a real Argon2id verification at the same parameters.
     */
    @Test
    void allThreeCategoriesCompareAgainstAnArgon2idHash() {
        passwordEncoder.lastEncodedPasswordSeen = null;
        refuse("ada", "wrong-password");
        String realAccountHash = passwordEncoder.lastEncodedPasswordSeen;

        passwordEncoder.lastEncodedPasswordSeen = null;
        refuse("nopass", "anything");
        String credentiallessHash = passwordEncoder.lastEncodedPasswordSeen;

        passwordEncoder.lastEncodedPasswordSeen = null;
        refuse("nobody", "anything");
        String unknownUsernameHash = passwordEncoder.lastEncodedPasswordSeen;

        assertThat(realAccountHash).startsWith("{argon2id}");
        assertThat(credentiallessHash).startsWith("{argon2id}");
        assertThat(unknownUsernameHash).startsWith("{argon2id}");
    }

    private void refuse(String username, String password) {
        assertThatThrownBy(() -> login.logIn(username, password))
                .isInstanceOf(AuthenticationException.class);
    }

    /** Wraps the real encoder, counting {@code matches} calls and recording the last comparand. */
    private static final class CountingPasswordEncoder implements PasswordEncoder {

        private final PasswordEncoder delegate;
        private final AtomicInteger matchCalls = new AtomicInteger();
        private volatile String lastEncodedPasswordSeen;

        CountingPasswordEncoder(PasswordEncoder delegate) {
            this.delegate = delegate;
        }

        @Override
        public String encode(CharSequence rawPassword) {
            return delegate.encode(rawPassword);
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            matchCalls.incrementAndGet();
            lastEncodedPasswordSeen = encodedPassword;
            return delegate.matches(rawPassword, encodedPassword);
        }
    }
}
