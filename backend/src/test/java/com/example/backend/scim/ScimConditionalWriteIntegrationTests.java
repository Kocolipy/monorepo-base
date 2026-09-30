package com.example.backend.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.example.backend.ContainerTestConfiguration;
import com.example.backend.auth.application.LoginService;
import com.example.backend.observability.RequestIdFilter;
import com.example.backend.scim.application.ConnectorAdministrationService;
import com.example.backend.scim.application.ScimUserService;
import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.ScimUserPatchOperation;
import com.example.backend.scim.domain.ScimVersionPrecondition;
import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.AuthenticationException;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Conditional User writes driven over the real filter chain, against a real Postgres and a real,
 * indexed Redis session store — the layers where the ticket's guarantees are actually decided:
 * the resource lock that turns a race into one {@code 412}, the columns a replacement leaves
 * alone, the history rows and their trim, and whether a live session survives.
 *
 * <p>Sessions are opened the way {@code AuthController} opens them: indexed by the User's stable
 * id. A revocation is therefore observed as a session the store no longer returns, not as a call a
 * fake recorded.
 */
@SpringBootTest
@Import(ContainerTestConfiguration.class)
@TestPropertySource(properties = "app.scim.enabled=true")
class ScimConditionalWriteIntegrationTests {

    private static final String USERS = "/scim/v2/Users";

    private static final String GROUPS = "/scim/v2/Groups";

    private static final String USER_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";

    private static final String PATCH_OP = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

    private static final MediaType SCIM_JSON = MediaType.valueOf("application/scim+json");

    private static final String FIRST_PASSWORD = "first-correct-horse";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ConnectorAdministrationService connectors;

    @Autowired
    private ScimUserService userService;

    @Autowired
    private LoginService login;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Autowired
    private RequestIdFilter requestIdFilter;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private final JsonMapper json = JsonMapper.builder().build();

    private MockMvc mvc;

    private UUID connectorA;

    private String tokenA;

    private String tokenB;

    private String readOnlyToken;

    private final List<UUID> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter, springSecurityFilterChain)
                .build();
        connectorA = connectors.create("Okta", "test-admin").id();
        UUID connectorB = connectors.create("Entra", "test-admin").id();
        tokenA = connectors.issueToken(connectorA, ConnectorTokenScope.READ_WRITE, null,
                "test-admin").presentedValue();
        tokenB = connectors.issueToken(connectorB, ConnectorTokenScope.READ_WRITE, null,
                "test-admin").presentedValue();
        readOnlyToken = connectors.issueToken(connectorA, ConnectorTokenScope.READ_ONLY, null,
                "test-admin").presentedValue();
    }

    @AfterEach
    void removeOnlyWhatThisTestCreated() {
        for (UUID id : created) {
            jdbc.update("DELETE FROM scim_resources WHERE id = ? AND reserved_name IS NULL", id);
        }
    }

    // ---- the precondition contract --------------------------------------------------------

    /** Absent is 428 with retry guidance, on every write against an existing resource. */
    @Test
    void every_existing_resource_write_without_if_match_is_refused_with_retry_guidance()
            throws Exception {
        UUID user = createUser("precondition-required");
        UUID group = createGroup("Precondition Required Group");
        long userVersion = version(USERS, user);

        for (MockHttpServletRequestBuilder write : List.of(
                withBody(put(USERS + "/" + user), minimalUser("precondition-required")),
                withBody(patch(USERS + "/" + user), patchOp(
                        "{\"op\":\"replace\",\"path\":\"active\",\"value\":false}")),
                withBody(put(GROUPS + "/" + group), "{\"schemas\":[\"urn:ietf:params:scim:"
                        + "schemas:core:2.0:Group\"],\"displayName\":\"Renamed\"}"),
                withBody(patch(GROUPS + "/" + group), patchOp(
                        "{\"op\":\"replace\",\"path\":\"displayName\",\"value\":\"R\"}")),
                delete(GROUPS + "/" + group))) {
            MvcResult refused = mvc.perform(as(tokenA, write)).andReturn();

            assertThat(refused.getResponse().getStatus()).isEqualTo(428);
            JsonNode error = body(refused);
            assertThat(error.get("status").asText()).isEqualTo("428");
            assertThat(error.has("scimType")).isFalse();
            assertThat(error.get("detail").asText()).contains("If-Match").contains("GET");
        }
        assertThat(version(USERS, user)).as("a refused precondition changes nothing")
                .isEqualTo(userVersion);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM scim_groups WHERE resource_id = ?", Integer.class, group))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "\"1\", \"2\"", "1", "W/1"})
    void a_wildcard_list_or_malformed_if_match_is_invalid_value(String header) throws Exception {
        UUID user = createUser("precondition-invalid-" + Math.abs(header.hashCode()));
        long before = version(USERS, user);

        MvcResult refused = mvc.perform(as(tokenA, withBody(patch(USERS + "/" + user),
                        patchOp("{\"op\":\"replace\",\"path\":\"active\",\"value\":false}")))
                .header(HttpHeaders.IF_MATCH, header)).andReturn();

        assertThat(refused.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(refused).get("scimType").asText()).isEqualTo("invalidValue");
        assertThat(version(USERS, user)).isEqualTo(before);
        assertThat(activeColumn(user)).isTrue();
    }

    @Test
    void a_stale_or_weak_if_match_is_a_conflict_that_changes_nothing_and_records_no_success()
            throws Exception {
        UUID user = createUser("precondition-stale");
        long before = version(USERS, user);

        for (String stale : List.of("\"" + (before + 1) + "\"", "W/\"" + before + "\"")) {
            MvcResult refused = mvc.perform(as(tokenA, withBody(put(USERS + "/" + user),
                            minimalUser("precondition-stale-renamed")))
                    .header(HttpHeaders.IF_MATCH, stale)).andReturn();

            assertThat(refused.getResponse().getStatus()).isEqualTo(412);
            assertThat(body(refused).get("status").asText()).isEqualTo("412");
        }
        assertThat(version(USERS, user)).isEqualTo(before);
        assertThat(userNameColumn(user)).isEqualTo("precondition-stale");
        assertThat(auditCount("SCIM_USER_REPLACE", user, "SUCCESS")).isZero();
    }

    /** Existence before the precondition: an id that names nothing is a 404 whatever is sent. */
    @Test
    void an_unknown_id_is_not_found_before_any_precondition_is_consulted() throws Exception {
        MvcResult result = mvc.perform(as(tokenA, withBody(patch(USERS + "/" + UUID.randomUUID()),
                patchOp("{\"op\":\"replace\",\"path\":\"active\",\"value\":false}")))).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    /** Authorization before the precondition: a read-only token is refused as such. */
    @Test
    void a_read_only_token_is_refused_for_its_scope_before_any_precondition_is_consulted()
            throws Exception {
        UUID user = createUser("precondition-read-only");

        MvcResult result = mvc.perform(as(readOnlyToken, withBody(patch(USERS + "/" + user),
                patchOp("{\"op\":\"replace\",\"path\":\"active\",\"value\":false}")))).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(activeColumn(user)).isTrue();
    }

    /**
     * The demo oracle: two connectors race a conditional update holding the same ETag. The
     * resource lock makes the second wait for the first to commit, so it then sees the version
     * the first produced — exactly one 200, exactly one 412, and exactly one version advance.
     * Repeated, because a race that happens to serialize once proves little.
     */
    @Test
    void two_connectors_racing_with_the_same_precondition_produce_exactly_one_success()
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 5; round++) {
                UUID user = createUser("race-" + round);
                long before = version(USERS, user);
                String etag = "\"" + before + "\"";
                CountDownLatch start = new CountDownLatch(1);

                Future<Integer> first = pool.submit(() -> {
                    start.await();
                    return status(as(tokenA, withBody(patch(USERS + "/" + user), patchOp(
                            "{\"op\":\"replace\",\"path\":\"displayName\",\"value\":\"From A\"}")))
                            .header(HttpHeaders.IF_MATCH, etag));
                });
                Future<Integer> second = pool.submit(() -> {
                    start.await();
                    return status(as(tokenB, withBody(patch(USERS + "/" + user), patchOp(
                            "{\"op\":\"replace\",\"path\":\"displayName\",\"value\":\"From B\"}")))
                            .header(HttpHeaders.IF_MATCH, etag));
                });
                start.countDown();

                assertThat(List.of(first.get(30, TimeUnit.SECONDS),
                                second.get(30, TimeUnit.SECONDS)))
                        .as("round " + round)
                        .containsExactlyInAnyOrder(200, 412);
                assertThat(version(USERS, user)).isEqualTo(before + 1);
                assertThat(auditCount("SCIM_USER_REPLACE", user, "SUCCESS")).isEqualTo(1);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- full replacement -----------------------------------------------------------------

    @Test
    void a_put_without_the_required_user_name_is_refused() throws Exception {
        UUID user = createUser("put-required");

        MvcResult refused = mvc.perform(conditional(tokenA, withBody(put(USERS + "/" + user),
                "{\"schemas\":[\"" + USER_SCHEMA + "\"],\"displayName\":\"No Name\"}"), user))
                .andReturn();

        assertThat(refused.getResponse().getStatus()).isEqualTo(400);
        assertThat(userNameColumn(user)).isEqualTo("put-required");
    }

    /**
     * Replacement: omitted optionals are cleared in the stored row, read-only attributes in the
     * body are ignored, the version advances exactly once, and the omitted password still logs in.
     */
    @Test
    void a_put_clears_omitted_optionals_ignores_read_only_attributes_and_keeps_the_password()
            throws Exception {
        UUID user = createUser("put-replace");
        UUID group = createGroupWith("Put Replace Members", user);
        // The optionals the PUT is about to omit really are stored, so their clearing is observed.
        Map<String, Object> stored = jdbc.queryForMap(
                "SELECT display_name, given_name, locale FROM scim_users WHERE resource_id = ?",
                user);
        assertThat(stored.values()).doesNotContainNull();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM scim_user_emails WHERE resource_id = ?", Integer.class, user))
                .isEqualTo(1);
        long before = version(USERS, user);

        // `groups` sent EMPTY while the User is a real member: honouring it would remove the row.
        MvcResult replaced = mvc.perform(conditional(tokenA, withBody(put(USERS + "/" + user), """
                {"schemas":["%s"],"userName":"put-replace","id":"%s",
                 "meta":{"version":"W/\\"999\\""},"groups":[]}"""
                .formatted(USER_SCHEMA, UUID.randomUUID())), user))
                .andReturn();

        assertThat(replaced.getResponse().getStatus()).isEqualTo(200);
        assertThat(replaced.getResponse().getHeader(HttpHeaders.ETAG))
                .isEqualTo("\"" + (before + 1) + "\"");
        JsonNode rendered = body(replaced);
        assertThat(rendered.get("id").asText()).isEqualTo(user.toString());
        assertThat(rendered.has("displayName")).isFalse();
        assertThat(rendered.get("meta").get("version").asText())
                .isEqualTo("\"" + (before + 1) + "\"");
        assertThat(version(USERS, user)).isEqualTo(before + 1);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT display_name, given_name, locale FROM scim_users WHERE resource_id = ?",
                user);
        assertThat(row.values()).containsOnlyNulls();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM scim_user_emails WHERE resource_id = ?", Integer.class, user))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM scim_group_members WHERE group_id = ? AND user_id = ?",
                Integer.class, group, user))
                .as("a submitted groups attribute is ignored: the membership survives").isEqualTo(1);
        assertThatCode(() -> login.logIn("put-replace", FIRST_PASSWORD))
                .as("an omitted password leaves the credential usable")
                .doesNotThrowAnyException();
    }

    /** A PUT may restate the stored alias but not change it — refused, not silently dropped. */
    @Test
    void a_put_changing_the_external_id_is_refused_and_restating_it_is_accepted()
            throws Exception {
        UUID user = createUser("put-alias");
        long before = version(USERS, user);

        MvcResult refused = mvc.perform(conditional(tokenA, withBody(put(USERS + "/" + user),
                "{\"schemas\":[\"" + USER_SCHEMA + "\"],\"userName\":\"put-alias\","
                        + "\"externalId\":\"a-different-alias\"}"), user)).andReturn();
        assertThat(refused.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(refused).get("scimType").asText()).isEqualTo("mutability");
        assertThat(version(USERS, user)).isEqualTo(before);

        MvcResult restated = mvc.perform(conditional(tokenA, withBody(put(USERS + "/" + user),
                "{\"schemas\":[\"" + USER_SCHEMA + "\"],\"userName\":\"put-alias\","
                        + "\"externalId\":\"ext-put-alias\"}"), user)).andReturn();
        assertThat(restated.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(restated).get("externalId").asText()).isEqualTo("ext-put-alias");
    }

    /** A replacement never writes the failure run, so a failed login counted before it survives. */
    @Test
    void a_replacement_leaves_the_failure_run_to_the_login_path() throws Exception {
        UUID user = createUser("put-failure-run");
        assertThatThrownBy(() -> login.logIn("put-failure-run", "not-the-password"))
                .isInstanceOf(AuthenticationException.class);

        mvc.perform(conditional(tokenA, withBody(put(USERS + "/" + user),
                minimalUser("put-failure-run")), user)).andReturn();

        assertThat(jdbc.queryForObject(
                "SELECT failed_login_attempts FROM scim_users WHERE resource_id = ?",
                Integer.class, user)).isEqualTo(1);
    }

    // ---- partial update -------------------------------------------------------------------

    @Test
    void a_no_op_patch_leaves_the_etag_and_last_modified_unchanged() throws Exception {
        UUID user = createUser("patch-no-op");
        JsonNode before = body(mvc.perform(as(tokenA, get(USERS + "/" + user))).andReturn());

        MvcResult patched = mvc.perform(conditional(tokenA, withBody(patch(USERS + "/" + user),
                patchOp("{\"op\":\"replace\",\"path\":\"active\",\"value\":true}",
                        "{\"op\":\"add\",\"path\":\"userName\",\"value\":\"patch-no-op\"}")),
                user)).andReturn();

        assertThat(patched.getResponse().getStatus()).isEqualTo(200);
        JsonNode after = body(patched);
        assertThat(after.get("meta").get("version")).isEqualTo(before.get("meta").get("version"));
        assertThat(after.get("meta").get("lastModified"))
                .isEqualTo(before.get("meta").get("lastModified"));
    }

    @Test
    void each_partial_update_refusal_has_its_scim_type_and_changes_nothing() throws Exception {
        UUID user = createUser("patch-refusals");
        long before = version(USERS, user);

        assertPatchRefused(user, "mutability",
                "{\"op\":\"remove\",\"path\":\"userName\"}");
        assertPatchRefused(user, "invalidPath",
                "{\"op\":\"replace\",\"path\":\"emails[type eq \\\"work\\\"\",\"value\":\"x\"}");
        assertPatchRefused(user, "noTarget",
                "{\"op\":\"remove\",\"path\":\"emails[type eq \\\"pager\\\"]\"}");

        assertThat(version(USERS, user)).isEqualTo(before);
    }

    /**
     * Atomicity, verified by re-reading: the first operation would deactivate and rename, the third
     * fails. The re-read User is exactly the one that existed before, and its session is intact.
     */
    @Test
    void a_multi_operation_patch_with_one_failure_restores_the_original() throws Exception {
        UUID user = createUser("patch-atomic");
        Session session = openSessionFor(user);
        JsonNode before = body(mvc.perform(as(tokenA, get(USERS + "/" + user))).andReturn());

        assertPatchRefused(user, "noTarget",
                "{\"op\":\"replace\",\"path\":\"active\",\"value\":false}",
                "{\"op\":\"replace\",\"path\":\"userName\",\"value\":\"patch-atomic-2\"}",
                "{\"op\":\"remove\",\"path\":\"emails[type eq \\\"pager\\\"]\"}");

        JsonNode after = body(mvc.perform(as(tokenA, get(USERS + "/" + user))).andReturn());
        assertThat(after).isEqualTo(before);
        assertThat(activeColumn(user)).isTrue();
        assertThat(sessionRepository.findById(session.getId())).isNotNull();
    }

    // ---- password history -----------------------------------------------------------------

    /**
     * Three remembered, current included, compared after normalization, on both update paths;
     * trimmed to three on every change; deleted with the User.
     */
    @Test
    void password_history_refuses_the_last_three_trims_to_three_and_goes_with_the_user()
            throws Exception {
        UUID user = createUser("history");

        assertThat(setPassword(user, "second-correct-horse", true)).isEqualTo(200);
        assertThat(setPassword(user, "cafe\u0301-third-horse", false)).isEqualTo(200);
        assertThat(historyRows(user)).isEqualTo(3);

        for (String reused : List.of(FIRST_PASSWORD, "second-correct-horse",
                "caf\u00e9-third-horse")) {
            for (boolean viaPatch : List.of(true, false)) {
                MvcResult refused = passwordWrite(user, reused, viaPatch);
                assertThat(refused.getResponse().getStatus()).as(reused).isEqualTo(400);
                assertThat(body(refused).get("scimType").asText()).isEqualTo("invalidValue");
                assertThat(body(refused).get("detail").asText()).doesNotContain(reused);
            }
        }
        assertThat(auditCount("SCIM_USER_REPLACE", user, "FAILURE")).isEqualTo(6);

        assertThat(setPassword(user, "fourth-correct-horse", true)).isEqualTo(200);
        assertThat(historyRows(user)).as("trimmed to three").isEqualTo(3);
        assertThat(setPassword(user, FIRST_PASSWORD, true))
                .as("the fourth most recent has aged out").isEqualTo(200);
        assertThatCode(() -> login.logIn("history", FIRST_PASSWORD)).doesNotThrowAnyException();

        jdbc.update("DELETE FROM scim_resources WHERE id = ?", user);
        assertThat(historyRows(user)).as("deleted with the User").isZero();
    }

    /** One normalization on every path: a password set decomposed logs in typed composed. */
    @Test
    void a_password_set_in_one_normalization_form_logs_in_with_the_other() throws Exception {
        UUID user = createUser("normalized-login");

        assertThat(setPassword(user, "cafe\u0301-and-a-horse", true)).isEqualTo(200);

        assertThatCode(() -> login.logIn("normalized-login", "caf\u00e9-and-a-horse"))
                .doesNotThrowAnyException();
    }

    // ---- session revocation against the real store ----------------------------------------

    /** The demo oracle's second half: a password change ends the prior session after commit. */
    @Test
    void a_password_change_revokes_the_prior_session_and_records_the_outcome() throws Exception {
        UUID user = createUser("revoke-password");
        Session session = openSessionFor(user);

        assertThat(setPassword(user, "second-correct-horse", true)).isEqualTo(200);

        assertThat(sessionRepository.findById(session.getId())).isNull();
        assertThat(jdbc.queryForObject("""
                SELECT changed_paths FROM audit_events
                 WHERE operation = 'SCIM_USER_REPLACE' AND subject_id = ? AND outcome = 'SUCCESS'
                """, String.class, user)).isEqualTo("password");
        Map<String, Object> revoked = jdbc.queryForMap("""
                SELECT outcome, actor_id, changed_paths, status_class FROM audit_events
                 WHERE operation = 'USER_SESSIONS_REVOKE' AND subject_id = ?""", user);
        assertThat(revoked).containsEntry("outcome", "SUCCESS")
                .containsEntry("actor_id", connectorA)
                .containsEntry("changed_paths", "password")
                .containsEntry("status_class", "ok");
    }

    /**
     * A rolled-back password change revokes nothing: the write runs inside a transaction that is
     * then rolled back, so the after-commit revocation never fires, the session survives and the
     * stored credential is the original one.
     */
    @Test
    void a_rolled_back_password_change_revokes_nothing() {
        UUID user = createUserThroughHttp("revoke-rollback");
        Session session = openSessionFor(user);
        String hashBefore = passwordHash(user);
        long version = versionColumn(user);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            userService.patch(
                    new AuthenticatedConnector(connectorA, UUID.randomUUID(),
                            ConnectorTokenScope.READ_WRITE),
                    user,
                    ScimVersionPrecondition.ofIfMatch(List.of("\"" + version + "\"")),
                    List.of(new ScimUserPatchOperation.SetPassword("rolled-back-horse")));
            status.setRollbackOnly();
        });

        assertThat(sessionRepository.findById(session.getId())).isNotNull();
        assertThat(passwordHash(user)).isEqualTo(hashBefore);
        assertThat(auditCount("USER_SESSIONS_REVOKE", user, "SUCCESS")).isZero();
    }

    @Test
    void deactivation_and_a_user_name_change_each_revoke_the_session() throws Exception {
        UUID deactivated = createUser("revoke-deactivate");
        UUID renamed = createUser("revoke-rename");
        Session first = openSessionFor(deactivated);
        Session second = openSessionFor(renamed);

        assertThat(status(conditional(tokenA, withBody(patch(USERS + "/" + deactivated),
                patchOp("{\"op\":\"replace\",\"path\":\"active\",\"value\":false}")),
                deactivated))).isEqualTo(200);
        assertThat(status(conditional(tokenA, withBody(patch(USERS + "/" + renamed),
                patchOp("{\"op\":\"replace\",\"path\":\"userName\",\"value\":\"revoke-renamed\"}")),
                renamed))).isEqualTo(200);

        assertThat(sessionRepository.findById(first.getId())).isNull();
        assertThat(sessionRepository.findById(second.getId())).isNull();
        assertThat(jdbc.queryForObject("""
                SELECT changed_paths FROM audit_events
                 WHERE operation = 'USER_SESSIONS_REVOKE' AND subject_id = ?""",
                String.class, deactivated)).isEqualTo("active");
        assertThat(jdbc.queryForObject("""
                SELECT changed_paths FROM audit_events
                 WHERE operation = 'USER_SESSIONS_REVOKE' AND subject_id = ?""",
                String.class, renamed)).isEqualTo("userName");
    }

    @Test
    void an_ordinary_profile_change_revokes_nothing() throws Exception {
        UUID user = createUser("revoke-profile-only");
        Session session = openSessionFor(user);

        assertThat(status(conditional(tokenA, withBody(patch(USERS + "/" + user), patchOp(
                "{\"op\":\"replace\",\"path\":\"displayName\",\"value\":\"Someone\"}",
                "{\"op\":\"add\",\"path\":\"emails\",\"value\":[{\"value\":\"a@b.example\"}]}")),
                user))).isEqualTo(200);

        assertThat(sessionRepository.findById(session.getId())).isNotNull();
        assertThat(auditCount("USER_SESSIONS_REVOKE", user, "SUCCESS")).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT changed_paths FROM audit_events
                 WHERE operation = 'SCIM_USER_REPLACE' AND subject_id = ? AND outcome = 'SUCCESS'
                """, String.class, user)).isEqualTo("displayName,emails");
    }

    // ---- helpers --------------------------------------------------------------------------

    private void assertPatchRefused(UUID user, String scimType, String... operations)
            throws Exception {
        MvcResult refused = mvc.perform(conditional(tokenA,
                withBody(patch(USERS + "/" + user), patchOp(operations)), user)).andReturn();
        assertThat(refused.getResponse().getStatus()).as(scimType).isEqualTo(400);
        assertThat(body(refused).get("scimType").asText()).isEqualTo(scimType);
    }

    private int setPassword(UUID user, String password, boolean viaPatch) throws Exception {
        return passwordWrite(user, password, viaPatch).getResponse().getStatus();
    }

    private MvcResult passwordWrite(UUID user, String password, boolean viaPatch)
            throws Exception {
        String value = json.writeValueAsString(password);
        MockHttpServletRequestBuilder write = viaPatch
                ? withBody(patch(USERS + "/" + user),
                        patchOp("{\"op\":\"replace\",\"path\":\"password\",\"value\":" + value + "}"))
                : withBody(put(USERS + "/" + user), "{\"schemas\":[\"" + USER_SCHEMA + "\"],"
                        + "\"userName\":" + json.writeValueAsString(userNameColumn(user))
                        + ",\"password\":" + value + "}");
        return mvc.perform(conditional(tokenA, write, user)).andReturn();
    }

    private UUID createUser(String userName) throws Exception {
        return createUserThroughHttp(userName);
    }

    private UUID createUserThroughHttp(String userName) {
        try {
            MvcResult result = mvc.perform(as(tokenA, withBody(post(USERS), """
                    {"schemas":["%s"],"userName":"%s","password":"%s","displayName":"Shown",
                     "externalId":"ext-%s",
                     "name":{"givenName":"Given"},"locale":"en-GB",
                     "emails":[{"value":"%s@work.example","type":"work","primary":true}]}"""
                    .formatted(USER_SCHEMA, userName, FIRST_PASSWORD, userName, userName))))
                    .andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(201);
            UUID id = UUID.fromString(body(result).get("id").asText());
            created.add(id);
            return id;
        } catch (Exception failed) {
            throw new AssertionError(failed);
        }
    }

    private UUID createGroup(String displayName) throws Exception {
        MvcResult result = mvc.perform(as(tokenA, withBody(post(GROUPS),
                "{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:Group\"],"
                        + "\"displayName\":\"" + displayName + "\"}"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(body(result).get("id").asText());
        created.add(id);
        return id;
    }

    private UUID createGroupWith(String displayName, UUID member) throws Exception {
        MvcResult result = mvc.perform(as(tokenA, withBody(post(GROUPS),
                "{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:Group\"],"
                        + "\"displayName\":\"" + displayName + "\","
                        + "\"members\":[{\"value\":\"" + member + "\"}]}"))).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(body(result).get("id").asText());
        created.add(id);
        return id;
    }

    private static String minimalUser(String userName) {
        return "{\"schemas\":[\"" + USER_SCHEMA + "\"],\"userName\":\"" + userName + "\"}";
    }

    private static String patchOp(String... operations) {
        return "{\"schemas\":[\"" + PATCH_OP + "\"],\"Operations\":["
                + String.join(",", operations) + "]}";
    }

    private static MockHttpServletRequestBuilder withBody(
            MockHttpServletRequestBuilder request, String body) {
        return request.contentType(SCIM_JSON).content(body);
    }

    private static MockHttpServletRequestBuilder as(
            String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /** The write with the resource's current version, as a conforming connector sends it. */
    private MockHttpServletRequestBuilder conditional(
            String token, MockHttpServletRequestBuilder request, UUID id) {
        return as(token, request).header(HttpHeaders.IF_MATCH, "\"" + versionColumn(id) + "\"");
    }

    private int status(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    /** The version as a connector reads it: the ETag of a GET. */
    private long version(String collection, UUID id) throws Exception {
        String etag = mvc.perform(as(tokenA, get(collection + "/" + id))).andReturn()
                .getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etag).isNotNull();
        return Long.parseLong(etag.replace("\"", ""));
    }

    private long versionColumn(UUID id) {
        return jdbc.queryForObject("SELECT version FROM scim_resources WHERE id = ?", Long.class, id);
    }

    private boolean activeColumn(UUID user) {
        return jdbc.queryForObject(
                "SELECT active FROM scim_users WHERE resource_id = ?", Boolean.class, user);
    }

    private String userNameColumn(UUID user) {
        return jdbc.queryForObject(
                "SELECT user_name FROM scim_users WHERE resource_id = ?", String.class, user);
    }

    private String passwordHash(UUID user) {
        return jdbc.queryForObject(
                "SELECT password_hash FROM scim_users WHERE resource_id = ?", String.class, user);
    }

    private int historyRows(UUID user) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM scim_user_password_history WHERE user_id = ?",
                Integer.class, user);
    }

    private int auditCount(String operation, UUID subject, String outcome) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM audit_events
                 WHERE operation = ? AND subject_id = ? AND outcome = ?""",
                Integer.class, operation, subject, outcome);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** Opens a session indexed by the User's stable id, exactly as a real login does. */
    private Session openSessionFor(UUID userId) {
        Session session = sessionRepository.createSession();
        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, userId.toString());
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                (FindByIndexNameSessionRepository<Session>) sessionRepository;
        repository.save(session);
        return session;
    }
}
