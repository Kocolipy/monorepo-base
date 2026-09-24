package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@SpringBootTest
// The assertion below is about how the context is built, not about a context
// that happens to be lying around: Spring's context cache is per JVM, so a
// reused instance would report on wiring performed by some earlier test class.
// Evicting first makes this class observe an actual startup.
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class BackendApplicationTests {

    /**
     * Injected rather than looked up so startup fails when the context has no
     * chain of its own. Dropping {@code SecurityConfig}'s contribution is
     * otherwise silent: Spring Security falls back to its default chain, and
     * the application still starts.
     */
    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void contextLoadsWithTheApplicationSecurityFilterChain() {
        assertThat(securityFilterChain).isNotNull();
        assertThat(securityFilterChain.getFilters()).isNotEmpty();
    }
}
