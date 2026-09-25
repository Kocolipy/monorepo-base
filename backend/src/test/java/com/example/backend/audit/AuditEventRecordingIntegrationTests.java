package com.example.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditRefusalReason;
import com.example.backend.auth.InMemoryAccountSessions;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.observability.RequestIdFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * Every audit-worthy thing this application already does, driven through the real
 * filter chain against a real Postgres, with the rows it produced read back out of
 * the database.
 *
 * <p>Driven end to end rather than against the services because the claim is about
 * what the application produces <em>today</em>: a service-level test would assert
 * that a use case called the trail, which is a different and weaker statement than
 * that a request arriving at this service leaves exactly one correctly-shaped row
 * behind. The rows are read with SQL rather than through the repository for the
 * same reason — what is asserted is the bytes that landed, not a mapping's opinion
 * of them.
 */
@SpringBootTest
@Import(ContainerTestConfiguration.class)
class AuditEventRecordingIntegrationTests {

    private static final String USER = "test-user";

    private static final String USER_PASSWORD = "test-password";

    private static final String ADMIN = "test-admin";

    private static final String EVENTS_BY_OPERATION =
            "SELECT * FROM audit_events WHERE operation = ? ORDER BY occurred_at";

    private static final String ALL_EVENTS = "SELECT * FROM audit_events";

    private static final String AUDIT_COLUMNS =
            "SELECT column_name FROM information_schema.columns"
            + " WHERE table_name = 'audit_events'";

    private static final String DELETE_ALL_EVENTS = "DELETE FROM audit_events";

    private static final String ASSUME_RETENTION_ROLE =
            "SET LOCAL ROLE backend_audit_retention";

    /** The session registry, in memory: this context has no Redis. */
    @TestConfiguration
    static class SessionRegistryConfiguration {

        @Bean
        @Primary
        InMemoryAccountSessions inMemoryAccountSessions() {
            return new InMemoryAccountSessions();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    /**
     * The filter that mints the correlation id an audit event records. Added to the
     * chain because without it the recorded {@code request_id} would be null and the
     * assertion that it is present would be asserting the harness rather than the
     * service.
     */
    @Autowired
    private RequestIdFilter requestIdFilter;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
        clearRecordedEvents();
        restore(USER);
        restore(ADMIN);
        clearRecordedEvents();
    }

    // One event per flow, correctly shaped

    @Test
    void anAcceptedLoginProducesExactlyOneEvent() throws Exception {
        logIn(USER, USER_PASSWORD).andExpect(status().isOk());

        Map<String, Object> event = only(AuditOperation.LOGIN_SUCCESS);
        assertThat(event).containsEntry("outcome", "SUCCESS");
        assertThat(event).containsEntry("actor_id", idOf(USER));
        assertThat(event).containsEntry("subject_id", idOf(USER));
        assertThat(event).containsEntry("resource_type", "Account");
        assertThat(event).containsEntry("status_class", "ok");
        assertThat(event).containsEntry("http_method", "POST");
        assertThat(event).containsEntry("http_path", "/api/auth/login");
        assertThat(event.get("request_id")).isNotNull();
        assertThat(event.get("occurred_at")).isNotNull();
    }

    @Test
    void aRefusedLoginProducesExactlyOneEventNamingTheReason() throws Exception {
        logIn(USER, "not-the-password").andExpect(status().isUnauthorized());

        Map<String, Object> event = only(AuditOperation.LOGIN_FAILURE);
        assertThat(event).containsEntry("outcome", "FAILURE");
        assertThat(event).containsEntry("actor_id", null);
        assertThat(event).containsEntry("subject_id", idOf(USER));
        assertThat(event).containsEntry("status_class", "client_error");
        assertThat(event).containsEntry(
                "error_code", AuditRefusalReason.BAD_CREDENTIALS.name());
        assertThat(event).containsEntry("changed_paths", "failedLoginAttempts");
    }

    /**
     * The submitted username names no account, so there is no subject — and the
     * submitted value is not recorded in its place, which is the whole point.
     */
    @Test
    void aRefusedLoginAgainstAnUnknownNameRecordsNoSubject() throws Exception {
        logIn("nobody-by-that-name", "whatever").andExpect(status().isUnauthorized());

        Map<String, Object> event = only(AuditOperation.LOGIN_FAILURE);
        assertThat(event).containsEntry("subject_id", null);
        assertThat(event).containsEntry(
                "error_code", AuditRefusalReason.UNKNOWN_ACCOUNT.name());
    }

    @Test
    void aLogoutProducesExactlyOneEvent() throws Exception {
        MockHttpSession session = (MockHttpSession) logIn(USER, USER_PASSWORD)
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();
        clearRecordedEvents();

        mvc.perform(withCsrf(delete("/api/auth/logout")).session(session))
                .andExpect(status().isNoContent());

        Map<String, Object> event = only(AuditOperation.LOGOUT);
        assertThat(event).containsEntry("actor_id", idOf(USER));
        assertThat(event).containsEntry("subject_id", idOf(USER));
        assertThat(event).containsEntry("http_method", "DELETE");
        assertThat(event).containsEntry("http_path", "/api/auth/logout");
    }

    /**
     * The fifth consecutive refusal is what locks the account, so the lockout is
     * recorded once and not on the four attempts before it or on any attempt made
     * during the window it opened.
     */
    @Test
    void reachingTheFailureLimitProducesExactlyOneLockoutEvent() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            logIn(USER, "not-the-password").andExpect(status().isUnauthorized());
        }
        // A sixth attempt, refused by the lockout rather than by the password,
        // neither extends the window nor records a second lockout.
        logIn(USER, USER_PASSWORD).andExpect(status().isUnauthorized());

        Map<String, Object> event = only(AuditOperation.LOCKOUT_SET);
        assertThat(event).containsEntry("subject_id", idOf(USER));
        assertThat(event).containsEntry("actor_id", null);
        assertThat(event).containsEntry(
                "changed_paths", "failedLoginAttempts,lockedAt");
        assertThat(rows(AuditOperation.LOGIN_FAILURE)).hasSize(6);
        assertThat(rows(AuditOperation.LOGIN_FAILURE))
                .extracting(row -> row.get("error_code"))
                .contains(AuditRefusalReason.ACCOUNT_LOCKED.name());
    }

    /**
     * A lockout imposed long ago is still in force, and no login attempt against it
     * records a lift — there is no unrequested lift left to record, so the only
     * {@code LOCKOUT_LIFT} row any flow can produce is an administrator's unlock.
     */
    @Test
    void aLockoutStandingSinceLongAgoProducesNoLiftAtTheNextAttempt() throws Exception {
        Account account = accounts.findByUsername(USER).orElseThrow();
        transactions.executeWithoutResult(status -> accounts.updateLockout(new Account(
                account.id(),
                account.username(),
                account.passwordHash(),
                account.role(),
                5,
                java.time.Instant.now().minusSeconds(3600),
                true,
                account.createdAt())));
        clearRecordedEvents();

        logIn(USER, USER_PASSWORD).andExpect(status().isUnauthorized());

        assertThat(rows(AuditOperation.LOCKOUT_LIFT)).isEmpty();
        assertThat(accounts.findByUsername(USER).orElseThrow().isLocked()).isTrue();
    }

    @Test
    void disablingAnAccountProducesExactlyOneEventNamingBothParties() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/{username}/disable", USER))
                        .session(sessionOf(ADMIN)))
                .andExpect(status().isOk());

        Map<String, Object> event = only(AuditOperation.ACCOUNT_DISABLE);
        assertThat(event).containsEntry("actor_id", idOf(ADMIN));
        assertThat(event).containsEntry("subject_id", idOf(USER));
        assertThat(event).containsEntry("changed_paths", "enabled");
        assertThat(event).containsEntry(
                "http_path", "/api/admin/accounts/{username}/disable");
    }

    @Test
    void enablingAnAccountProducesExactlyOneEventNamingBothParties() throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/{username}/enable", USER))
                        .session(sessionOf(ADMIN)))
                .andExpect(status().isOk());

        Map<String, Object> event = only(AuditOperation.ACCOUNT_ENABLE);
        assertThat(event).containsEntry("actor_id", idOf(ADMIN));
        assertThat(event).containsEntry("subject_id", idOf(USER));
        assertThat(event).containsEntry("changed_paths", "enabled");
    }

    @Test
    void unlockingAnAccountProducesExactlyOneLiftEventNamingTheAdministrator()
            throws Exception {
        mvc.perform(withCsrf(post("/api/admin/accounts/{username}/unlock", USER))
                        .session(sessionOf(ADMIN)))
                .andExpect(status().isOk());

        Map<String, Object> event = only(AuditOperation.LOCKOUT_LIFT);
        assertThat(event).containsEntry("error_code", null);
        assertThat(event).containsEntry("actor_id", idOf(ADMIN));
        assertThat(event).containsEntry("subject_id", idOf(USER));
    }

    // Redaction

    /**
     * Every flow at once, then a search of every text column in every recorded row
     * for the values that must never be in one.
     *
     * <p>A search for absence proves nothing unless the thing searched was
     * populated, so the same scan is first asked for a value that <em>is</em> there
     * — the resource type — and has to find it. Without that check this test would
     * pass against an empty table.
     */
    @Test
    void noRecordedEventBodyCarriesAUsernameOrAPassword() throws Exception {
        logIn(USER, USER_PASSWORD).andExpect(status().isOk());
        logIn(USER, "wrong-" + USER_PASSWORD).andExpect(status().isUnauthorized());
        logIn("no-such-account", "irrelevant").andExpect(status().isUnauthorized());
        mvc.perform(withCsrf(post("/api/admin/accounts/{username}/disable", USER))
                        .session(sessionOf(ADMIN)))
                .andExpect(status().isOk());
        mvc.perform(withCsrf(post("/api/admin/accounts/{username}/enable", USER))
                        .session(sessionOf(ADMIN)))
                .andExpect(status().isOk());
        mvc.perform(withCsrf(post("/api/admin/accounts/{username}/unlock", USER))
                        .session(sessionOf(ADMIN)))
                .andExpect(status().isOk());

        assertThat(allEvents()).hasSizeGreaterThanOrEqualTo(6);
        // The scan works: a value that is present is found.
        assertThat(rowsContaining("Account")).isNotZero();

        assertThat(rowsContaining(USER)).isZero();
        assertThat(rowsContaining(ADMIN)).isZero();
        assertThat(rowsContaining(USER_PASSWORD)).isZero();
        assertThat(rowsContaining("no-such-account")).isZero();
        assertThat(rowsContaining("$argon2")).isZero();
        assertThat(rowsContaining("Bearer")).isZero();
    }

    /**
     * The table has no column a readable identifier could be written into in the
     * first place. Asserted against the deployed schema rather than the entity, so
     * a column added by a migration without a mapping is caught too.
     */
    @Test
    void theAuditTableHasNoColumnThatCouldHoldACredential() {
        List<String> columns = jdbc.queryForList(AUDIT_COLUMNS, String.class);

        assertThat(columns).isNotEmpty();
        assertThat(columns).noneSatisfy(column -> assertThat(column).containsAnyOf(
                "username", "user_name", "password", "secret", "token", "bearer",
                "credential", "cookie", "hash"));
        assertThat(columns).contains("actor_id", "subject_id", "resource_id");
    }

    // Helpers

    private org.springframework.test.web.servlet.ResultActions logIn(
            String username, String password) throws Exception {
        return mvc.perform(withCsrf(post("/api/auth/login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password
                        + "\"}"));
    }

    private Map<String, Object> only(AuditOperation operation) {
        List<Map<String, Object>> found = rows(operation);
        assertThat(found).as("exactly one %s event", operation).hasSize(1);
        return found.get(0);
    }

    private List<Map<String, Object>> rows(AuditOperation operation) {
        return jdbc.queryForList(EVENTS_BY_OPERATION, operation.name());
    }

    private List<Map<String, Object>> allEvents() {
        return jdbc.queryForList(ALL_EVENTS);
    }

    /**
     * How many recorded rows carry {@code value} in any textual field.
     *
     * <p>Scanned in Java over every row the table holds rather than with a query
     * built from the column list: a search for absence has to cover columns nobody
     * remembered to name, and assembling SQL from catalog output to do it would be
     * an injection shape in a test for a redaction rule.
     */
    private long rowsContaining(String value) {
        return allEvents().stream()
                .filter(row -> row.values().stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .anyMatch(text -> text.contains(value)))
                .count();
    }

    private UUID idOf(String username) {
        return accounts.findByUsername(username).orElseThrow().id();
    }

    /**
     * Removes recorded events between tests, through the retention role — the
     * application's own role cannot, which is the arrangement
     * {@code AuditAppendOnlyIntegrationTests} proves.
     */
    private void clearRecordedEvents() {
        transactions.executeWithoutResult(status -> {
            jdbc.execute(ASSUME_RETENTION_ROLE);
            jdbc.update(DELETE_ALL_EVENTS);
        });
    }

    /**
     * Returns the account to enabled, unlocked and with no failure run recorded.
     *
     * <p>Wrapped in a transaction because the narrow administrative writes are
     * modifying queries: without one they have no {@code EntityManager} to flush.
     */
    private void restore(String username) {
        transactions.executeWithoutResult(status -> {
            Account account = accounts.findByUsername(username).orElseThrow();
            accounts.updateEnabled(new Account(
                    account.id(), account.username(), account.passwordHash(), account.role(),
                    0, null, true, account.createdAt()));
            accounts.updateLockout(new Account(
                    account.id(), account.username(), account.passwordHash(), account.role(),
                    0, null, true, account.createdAt()));
        });
    }

    private MockHttpSession sessionOf(String username) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new TestingAuthenticationToken(username, null, "ROLE_ADMIN"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
        return session;
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
        CsrfToken token = csrfTokenRepository.generateToken(new MockHttpServletRequest());
        return request
                .cookie(new Cookie("XSRF-TOKEN", token.getToken()))
                .header("X-XSRF-TOKEN", token.getToken());
    }
}
