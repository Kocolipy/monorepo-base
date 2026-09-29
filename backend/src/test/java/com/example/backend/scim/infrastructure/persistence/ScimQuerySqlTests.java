package com.example.backend.scim.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.scim.domain.ScimFilterParser;
import com.example.backend.scim.domain.ScimPageRequest;
import com.example.backend.scim.domain.ScimQuery;
import com.example.backend.scim.domain.ScimResourceType;
import com.example.backend.scim.domain.ScimSort;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The parameterization criterion, held as a property of the translator's OUTPUT: whatever a
 * filter's literal contains, it never appears in the statement text, and it does appear —
 * verbatim, or as the escaped {@code LIKE} pattern built from it — among the bound values.
 *
 * <p>The integration fixtures prove the statements return the right rows; this proves how they
 * got there. A translator that concatenated a quoted value would pass most row-level fixtures and
 * fail here on the first one.
 */
class ScimQuerySqlTests {

    private static final Set<ScimResourceType> USERS = Set.of(ScimResourceType.USER);

    private static final Set<ScimResourceType> BOTH =
            Set.of(ScimResourceType.USER, ScimResourceType.GROUP);

    private static final UUID CONNECTOR = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private static final String BASE_URI = "https://scim.example/scim/v2";

    private static ScimQuery query(String filter, Set<ScimResourceType> types) {
        return new ScimQuery(types, ScimFilterParser.parse(filter, types), null,
                ScimPageRequest.FIRST_PAGE);
    }

    /**
     * Literals built to look like the statement's own syntax. None is a fragment of the fixed SQL
     * this class emits, so "does not contain" is a test of where the literal went rather than of
     * whether the translator happens to use the same characters.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "x' OR '1'='1",
            "'; DROP TABLE scim_users; --",
            "qp-alice' --",
            "O'Brien",
            ":connectorIdX",
            ":p99x",
            "?",
            "$1",
            "a\\b",
            "50%_off",
            "\"quoted\"",
            "a) OR (1=1",
            "*/ SELECT 1 /*",
            "é\u2028\u00a0"})
    void a_literal_never_becomes_statement_text(String literal) {
        String json = literal.replace("\\", "\\\\").replace("\"", "\\\"");
        for (String operator : new String[] {"eq", "ne", "co", "sw", "ew", "gt", "le"}) {
            for (String path : new String[] {"userName", "displayName", "emails.value", "externalId"}) {
                String filter = path + " " + operator + " \"" + json + "\" or emails[type "
                        + operator + " \"" + json + "\"]";
                ScimQuery query = query(filter, BOTH);

                ScimQuerySql count = new ScimQuerySql(CONNECTOR, BASE_URI);
                String countSql = count.count(query);
                ScimQuerySql page = new ScimQuerySql(CONNECTOR, BASE_URI);
                String pageSql = page.page(query);

                assertThat(countSql).as(filter).doesNotContain(literal);
                assertThat(pageSql).as(filter).doesNotContain(literal);
                String bound = operator.equals("co") ? "%" + ScimQuerySql.escapeLike(literal) + "%"
                        : operator.equals("sw") ? ScimQuerySql.escapeLike(literal) + "%"
                        : operator.equals("ew") ? "%" + ScimQuerySql.escapeLike(literal)
                        : literal;
                assertThat(count.parameters()).as(filter).containsValue(bound);
                assertThat(page.parameters()).as(filter).containsValue(bound);
            }
        }
    }

    /** The same property over random printable text, seeded so a failure reproduces. */
    @Test
    void no_random_literal_becomes_statement_text() {
        Random random = new Random(17L);
        for (int round = 0; round < 2_000; round++) {
            // A leading § cannot occur in the statement's fixed text, so any match is the literal.
            StringBuilder literal = new StringBuilder("§");
            int length = 1 + random.nextInt(12);
            for (int i = 0; i < length; i++) {
                literal.append((char) (0x21 + random.nextInt(0x5e)));
            }
            String value = literal.toString();
            String json = value.replace("\\", "\\\\").replace("\"", "\\\"");
            ScimQuery query = query("userName eq \"" + json + "\"", USERS);
            ScimQuerySql sql = new ScimQuerySql(CONNECTOR, BASE_URI);

            assertThat(sql.page(query)).doesNotContain(value);
            assertThat(sql.parameters()).containsValue(value);
        }
    }

    @Test
    void a_like_pattern_escapes_its_own_metacharacters() {
        assertThat(ScimQuerySql.escapeLike("50%_off\\")).isEqualTo("50\\%\\_off\\\\");
        assertThat(ScimQuerySql.escapeLike("plain")).isEqualTo("plain");
    }

    /** Case sensitivity follows the attribute: lower() on both sides only where caseExact=false. */
    @Test
    void case_insensitive_attributes_lower_both_sides_and_case_exact_ones_neither() {
        ScimQuerySql insensitive = new ScimQuerySql(CONNECTOR, BASE_URI);
        String userName = insensitive.count(query("userName eq \"A\"", USERS));
        ScimQuerySql exact = new ScimQuerySql(CONNECTOR, BASE_URI);
        String displayName = exact.count(query("displayName eq \"A\"", USERS));

        assertThat(userName).contains("lower(u.user_name) = lower(:p0)");
        assertThat(displayName).contains("u.display_name = :p0").doesNotContain("lower(");
    }

    @Test
    void a_datetime_literal_is_bound_as_a_utc_timestamp() {
        ScimQuerySql sql = new ScimQuerySql(CONNECTOR, BASE_URI);
        sql.count(query("meta.created ge \"2011-05-13T04:42:34+08:00\"", USERS));

        assertThat(sql.parameters().get("p0"))
                .isEqualTo(OffsetDateTime.of(2011, 5, 12, 20, 42, 34, 0, ZoneOffset.UTC));
    }

    @Test
    void the_connector_and_base_uri_are_bound_values_too() {
        ScimQuerySql sql = new ScimQuerySql(CONNECTOR, BASE_URI);
        String statement = sql.count(query("externalId eq \"e\" and meta.location pr", USERS));

        assertThat(statement).contains(":connectorId").contains(":baseUri")
                .doesNotContain(CONNECTOR.toString()).doesNotContain(BASE_URI);
        assertThat(sql.parameters()).containsEntry("connectorId", CONNECTOR)
                .containsEntry("baseUri", BASE_URI);
    }

    /** An attribute a type lacks renders as the constant its absence implies, in that type's branch. */
    @Test
    void an_attribute_a_type_lacks_is_a_constant_in_that_branch() {
        ScimQuerySql sql = new ScimQuerySql(CONNECTOR, BASE_URI);
        String statement = sql.count(query(
                "userName eq \"a\" or userName ne \"b\" or members pr or emails[type pr]", BOTH));

        String userBranch = statement.substring(0, statement.indexOf(" + "));
        String groupBranch = statement.substring(statement.indexOf(" + "));
        assertThat(groupBranch).doesNotContain("user_name").doesNotContain("scim_user_emails")
                .contains("(FALSE OR (NOT FALSE))");
        assertThat(userBranch).doesNotContain("mm.group_id").contains("FALSE");
    }

    @Test
    void a_page_orders_by_the_sort_key_then_id_and_binds_limit_and_offset() {
        ScimQuery sorted = new ScimQuery(
                USERS, null,
                ScimSort.of("displayName", "descending", USERS),
                new ScimPageRequest(11, 5));
        ScimQuerySql sql = new ScimQuerySql(CONNECTOR, BASE_URI);

        String statement = sql.page(sorted);

        assertThat(statement).endsWith(
                "ORDER BY q.sort_key COLLATE \"C\" DESC NULLS FIRST, q.id LIMIT :limit OFFSET :offset");
        assertThat(sql.parameters()).containsEntry("limit", 5).containsEntry("offset", 10);
    }

    @Test
    void an_unsorted_page_uses_the_default_order() {
        ScimQuerySql sql = new ScimQuerySql(CONNECTOR, BASE_URI);

        String statement = sql.page(new ScimQuery(BOTH, null, null, ScimPageRequest.FIRST_PAGE));

        assertThat(statement)
                .contains("0 AS type_order, u.normalized_user_name AS default_key")
                .contains("1 AS type_order, g.normalized_display_name AS default_key")
                .endsWith("ORDER BY q.type_order, q.default_key, q.id LIMIT :limit OFFSET :offset");
    }

    @Test
    void a_non_text_sort_key_has_no_collation_and_ascending_puts_missing_last() {
        ScimQuery sorted = new ScimQuery(USERS, null, ScimSort.of("meta.created", null, USERS),
                ScimPageRequest.FIRST_PAGE);
        ScimQuerySql sql = new ScimQuerySql(CONNECTOR, BASE_URI);

        assertThat(sql.page(sorted)).contains("r.created_at AS sort_key")
                .contains("ORDER BY q.sort_key ASC NULLS LAST, q.id");
    }

    /**
     * Presence by kind: text is present only when non-empty (SCIM treats "" as unassigned), any
     * other kind when non-null, and meta always. Text-level because an empty string cannot be
     * stored through the API to show the difference end to end.
     */
    @Test
    void presence_renders_by_the_attributes_kind() {
        assertThat(new ScimQuerySql(CONNECTOR, BASE_URI).count(query("displayName pr", USERS)))
                .contains("WHERE (u.display_name IS NOT NULL AND u.display_name <> ''))");
        assertThat(new ScimQuerySql(CONNECTOR, BASE_URI).count(query("active pr", USERS)))
                .contains("WHERE (u.active IS NOT NULL))");
        assertThat(new ScimQuerySql(CONNECTOR, BASE_URI).count(query("meta pr", USERS)))
                .contains("WHERE TRUE)");
    }

    /** In a base search the branch lacking the sort attribute projects a NULL of the other's type. */
    @Test
    void a_branch_lacking_the_sort_attribute_projects_a_typed_null() {
        ScimQuerySql text = new ScimQuerySql(CONNECTOR, BASE_URI);
        ScimQuerySql bool = new ScimQuerySql(CONNECTOR, BASE_URI);
        ScimQuerySql members = new ScimQuerySql(CONNECTOR, BASE_URI);

        assertThat(text.page(new ScimQuery(BOTH, null, ScimSort.of("userName", null, BOTH), null)))
                .contains("CAST(NULL AS text) AS sort_key");
        assertThat(bool.page(new ScimQuery(BOTH, null, ScimSort.of("active", null, BOTH), null)))
                .contains("CAST(NULL AS boolean) AS sort_key")
                .doesNotContain("COLLATE");
        assertThat(members.page(new ScimQuery(
                        BOTH, null, ScimSort.of("members.display", null, BOTH), null)))
                .contains("CAST(NULL AS text) AS sort_key")
                .contains("ORDER BY mu.normalized_user_name, mu.resource_id LIMIT 1");
    }
}
