package com.example.backend.scim.config;

import com.example.backend.scim.domain.PasswordAcceptance;
import com.example.backend.scim.domain.PasswordHasher;
import com.example.backend.scim.domain.ScimPasswordHistoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Wires the one {@link PasswordAcceptance} every password-setting path shares, over the password
 * history adapter and the deployment's {@link PasswordEncoder} — the normalizing Argon2id encoder,
 * so a candidate is hashed and compared in the form it is stored in.
 */
@Configuration
public class ScimPasswordAcceptanceConfig {

    @Bean
    public PasswordAcceptance passwordAcceptance(
            ScimPasswordHistoryRepository passwordHistory, PasswordEncoder passwordEncoder) {
        return new PasswordAcceptance(passwordHistory, hasher(passwordEncoder));
    }

    /**
     * The domain's view of the encoder: hash and compare, nothing more. Public so a test builds
     * its {@link PasswordAcceptance} through the same adapter the container does.
     */
    public static PasswordHasher hasher(PasswordEncoder passwordEncoder) {
        return new PasswordHasher() {
            @Override
            public String hash(String password) {
                return passwordEncoder.encode(password);
            }

            @Override
            public boolean matches(String candidate, String storedHash) {
                return passwordEncoder.matches(candidate, storedHash);
            }
        };
    }
}
