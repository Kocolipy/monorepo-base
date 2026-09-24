package com.example.backend.auth.application;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.time.Clock;
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

    public void seedDefaults(
            String username,
            String password,
            String adminUsername,
            String adminPassword) {
        seedIfAbsent(username, password, AccountRole.USER);
        seedIfAbsent(adminUsername, adminPassword, AccountRole.ADMIN);
    }

    /**
     * Reports the account to Spring Security, including whether it is currently
     * locked. Carrying the lockout here is what rejects a locked account with its
     * correct password: {@code DaoAuthenticationProvider} checks account status
     * before it checks the password, so the credentials are never even compared.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        Account account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .roles(account.role().name())
                .accountLocked(account.isLocked(clock.instant()))
                .build();
    }

    private void seedIfAbsent(String username, String password, AccountRole role) {
        if (accounts.findByUsername(username).isEmpty()) {
            accounts.save(new Account(username, passwordEncoder.encode(password), role));
        }
    }
}
