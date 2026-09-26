package com.example.backend.auth.infrastructure.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.auth.application.AccountAdministrationService;
import com.example.backend.auth.application.LoginAttemptService;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.auth.domain.AccountRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The Redis half of "session-revocation-on-disable still works under the new
 * key", proved against a real, indexed Spring Session repository rather than the
 * {@link com.example.backend.auth.InMemoryAccountSessions} fake other tests use.
 *
 * <p>{@code AGENTS.md} requires an integration-level check against Redis for
 * changes to Redis-backed session persistence — a servlet-mock controller test
 * cannot see the indexed lookup {@link AccountSessionsAdapter} depends on, only
 * that a fake reported the right calls.
 *
 * <p>This class writes the session index entry the same way {@code AuthController}
 * does on a real login: the account's stable id, not its username, into
 * {@link FindByIndexNameSessionRepository#PRINCIPAL_NAME_INDEX_NAME}. That is
 * exactly the behavior the migration and the rekeyed adapter exist to prove —
 * a session survives a rename because nothing about it was ever keyed by the
 * name that changed.
 */
@SpringBootTest
@Import(com.example.backend.ContainerTestConfiguration.class)
@DirtiesContext
class RedisSessionRevocationIntegrationTests {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8.2-alpine"))
                    .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private AccountAdministrationService administration;

    @Autowired
    private LoginAttemptService attempts;

    @Value("${app.auth.lockout.max-attempts}")
    private int maxAttempts;

    @Autowired
    private AccountSessionsAdapter sessionsAdapter;

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    /**
     * The full journey the acceptance criterion names: an account is renamed
     * directly against the fixture (no rename API exists yet), and both its
     * counter and its live Redis session must still be found afterward, under
     * the same stable id the session was opened with.
     */
    @Test
    void aLiveSessionSurvivesAUsernameChangeMadeDirectlyAgainstTheFixture() {
        Account created = accounts.save(
                new Account("before-rename", "hash", AccountRole.USER));
        UUID stableId = created.id();

        Session session = openSessionFor(stableId);

        Account renamed = new Account(
                created.id(),
                "after-rename",
                created.passwordHash(),
                created.role(),
                created.failedLoginAttempts(),
                created.lockedAt(),
                created.enabled(),
                created.createdAt());
        accounts.save(renamed);

        assertThat(sessionRepository.findById(session.getId())).isNotNull();
        assertThat(sessionsAdapter.revokeAll(stableId)).isEqualTo(1);
        assertThat(sessionRepository.findById(session.getId())).isNull();
    }

    /**
     * Session-revocation-on-disable, driven through the real administration use
     * case and the real Redis-indexed repository — not the in-memory fake other
     * controller-level tests substitute. Disabling must still end the account's
     * session when the index is keyed by its stable id rather than its username.
     */
    @Test
    void disablingAnAccountRevokesItsRealRedisBackedSession() {
        Account created = accounts.save(
                new Account("session-disable-target", "hash", AccountRole.USER));
        UUID stableId = created.id();
        Session session = openSessionFor(stableId);

        administration.disable(created.username(), "some-other-admin");

        assertThat(sessionRepository.findById(session.getId())).isNull();
    }

    /**
     * Session-revocation-on-lockout, driven through the real login-attempt
     * counter and the real Redis-indexed repository. The lock is imposed by
     * counting failures, not by writing the row directly, so the revocation is
     * observed on the path a real brute-force attempt takes: the account's live
     * session must be gone once the lock lands, or a locked account would keep
     * acting through a session it already held.
     */
    @Test
    void imposingALockoutRevokesTheAccountsRealRedisBackedSession() {
        Account created = accounts.save(
                new Account("session-lockout-target", "hash", AccountRole.USER));
        Session session = openSessionFor(created.id());

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            attempts.recordFailure(created.username(), AuditRefusalReason.BAD_CREDENTIALS);
        }

        assertThat(accounts.findByUsername(created.username()).orElseThrow().isLocked())
                .isTrue();
        assertThat(sessionRepository.findById(session.getId())).isNull();
    }

    /** Opens a session indexed by the account's stable id, as a real login would. */
    private Session openSessionFor(UUID accountId) {
        Session session = sessionRepository.createSession();
        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                accountId.toString());
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                (FindByIndexNameSessionRepository<Session>) sessionRepository;
        repository.save(session);
        return session;
    }
}
