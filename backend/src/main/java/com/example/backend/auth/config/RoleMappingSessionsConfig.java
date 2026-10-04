package com.example.backend.auth.config;

import com.example.backend.auth.application.RoleMappingSessionService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ends, at startup, every session issued under a role mapping other than the running one.
 *
 * <p>A runner rather than a request-time check: the mapping cannot change while the process runs,
 * so the only moment a session can start disagreeing with it is a deploy, and one pass over the
 * session store then is the whole of the work. A failure fails startup, like the mapping's own
 * validation does — a deployment that cannot enforce its new mapping on the sessions it inherited
 * should not start serving them.
 */
@Configuration
public class RoleMappingSessionsConfig {

    @Bean
    ApplicationRunner revokeSessionsIssuedUnderAnotherMapping(RoleMappingSessionService sessions) {
        return arguments -> sessions.revokeSessionsIssuedUnderAnotherMapping();
    }
}
