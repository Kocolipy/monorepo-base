package com.example.backend.auth.application;

import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
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

    public AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    public void seedDefaults(
            String username,
            String password,
            String adminUsername,
            String adminPassword) {
        seedIfAbsent(username, password, AccountRole.USER);
        seedIfAbsent(adminUsername, adminPassword, AccountRole.ADMIN);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Account account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return User.withUsername(account.username())
                .password(account.passwordHash())
                .roles(account.role().name())
                .build();
    }

    private void seedIfAbsent(String username, String password, AccountRole role) {
        if (accounts.findByUsername(username).isEmpty()) {
            accounts.save(new Account(username, passwordEncoder.encode(password), role));
        }
    }
}
