package com.example.backend.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.backend.ContainerTestConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * What {@code V5__permanent_lockout.sql} does to rows that already existed, which
 * is the one part of the change no other test can reach: Flyway runs against a
 * fresh database everywhere else, so the migration's data clauses execute over
 * zero rows and could be deleted without any suite noticing.
 *
 * <p>The statements are read out of the shipped migration file and executed as
 * written, not paraphrased here. A paraphrase would let the file and the assertion
 * drift apart in exactly the case that matters — someone editing the migration —
 * and would prove only that this test's own SQL behaves as this test expects.
 *
 * <p>They run against a temporary table named {@code accounts} on a connection
 * this class opens itself, straight to the container and deliberately NOT from the
 * application's {@code DataSource}. That matters more than it looks: a temporary
 * table shadows the real one for its own session, and a pooled connection is handed
 * back still carrying it, so probing through the pool makes every later test that
 * touches {@code accounts} read this table instead — Hibernate then fails on the
 * columns a V4-shaped probe does not have. A private connection is closed here and
 * takes the table with it.
 *
 * <p>The temporary table buys real PostgreSQL semantics for {@code now()}, the
 * {@code CASE} expressions and the column rename without replaying V1-V4 — whose
 * grants to {@code backend_app} and {@code search_path}-pinned trigger function
 * belong to a database build, not to this question.
 */
@SpringBootTest
@Import(ContainerTestConfiguration.class)
class PermanentLockoutMigrationTests {

    private static final Path MIGRATION =
            Path.of("src/main/resources/db/migration/V5__permanent_lockout.sql");

    /** The threshold the lockout is configured with, as any released row exceeds. */
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private PostgreSQLContainer<?> postgres;

    /** A connection of this test's own: unpooled, so its temporary table is private. */
    private Connection openPrivateConnection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    /**
     * The row this migration exists to protect: a window that had already run out.
     * Under the old rule the account was free to log in and its next failure
     * started a fresh run, because the count was read as stale once the window
     * ended. With that branch gone, releasing the lock without clearing the run
     * would leave the account one failed login from a permanent lock.
     */
    @Test
    void releasesAnExpiredLockoutAndClearsTheRunThatProducedIt() throws Exception {
        try (Connection connection = openPrivateConnection()) {
            givenAccountsAtVersionFour(connection);
            insert(connection, "expired", MAX_ATTEMPTS, "now() - interval '1 hour'");

            applyMigration(connection);

            assertThat(lockedAt(connection, "expired")).isNull();
            assertThat(failedAttempts(connection, "expired"))
                    .describedAs("a released account must start from a clean run")
                    .isZero();
        }
    }

    /**
     * The other kind of row: still inside its window, so it WAS locked and stays
     * locked — now permanently. Its count is the evidence for the lock it is still
     * serving, so clearing it here would erase the reason the account is locked.
     */
    @Test
    void keepsAStandingLockoutAndTheRunBehindIt() throws Exception {
        try (Connection connection = openPrivateConnection()) {
            givenAccountsAtVersionFour(connection);
            insert(connection, "standing", MAX_ATTEMPTS, "now() + interval '10 minutes'");

            applyMigration(connection);

            assertThat(lockedAt(connection, "standing")).isNotNull();
            assertThat(failedAttempts(connection, "standing")).isEqualTo(MAX_ATTEMPTS);
        }
    }

    /**
     * An account that was never locked is not touched at all — neither released nor
     * reset. A run in progress below the threshold survives the migration, because
     * nothing about it was expiry-dependent.
     */
    @Test
    void leavesAnAccountThatWasNeverLockedExactlyAsItWas() throws Exception {
        try (Connection connection = openPrivateConnection()) {
            givenAccountsAtVersionFour(connection);
            insert(connection, "never-locked", 2, "null");

            applyMigration(connection);

            assertThat(lockedAt(connection, "never-locked")).isNull();
            assertThat(failedAttempts(connection, "never-locked")).isEqualTo(2);
        }
    }

    /** The column the application reads is the renamed one, not a copy beside it. */
    @Test
    void renamesTheColumnRatherThanAddingOne() throws Exception {
        try (Connection connection = openPrivateConnection()) {
            givenAccountsAtVersionFour(connection);

            applyMigration(connection);

            assertThat(columnNames(connection)).contains("locked_at").doesNotContain("locked_until");
        }
    }

    /**
     * The accounts table as V1-V4 leave it, in the columns this migration reads.
     *
     * <p>The drop is schema-qualified to {@code pg_temp} deliberately: the
     * {@code DataSource} is a pool, so a connection handed back by an earlier test
     * still carries that test's temporary table, and an unqualified
     * {@code DROP TABLE IF EXISTS accounts} would resolve to the REAL table on a
     * connection that happened to have no temporary one.
     */
    private static void givenAccountsAtVersionFour(Connection connection) throws SQLException {
        execute(connection, "DROP TABLE IF EXISTS pg_temp.accounts");
        execute(
                connection,
                "CREATE TEMPORARY TABLE accounts ("
                        + " username VARCHAR(255) NOT NULL,"
                        + " failed_login_attempts INTEGER NOT NULL,"
                        + " locked_until TIMESTAMPTZ)");
    }

    private static void insert(
            Connection connection, String username, int attempts, String lockedUntilExpression)
            throws SQLException {
        execute(
                connection,
                "INSERT INTO accounts (username, failed_login_attempts, locked_until) VALUES ('"
                        + username + "', " + attempts + ", " + lockedUntilExpression + ")");
    }

    /**
     * Every statement of the shipped migration, in order. Comment lines are dropped
     * first, then the text is split on the semicolons that are NOT inside a string
     * literal — the file's own column comment contains one ("failure run; NULL when
     * …"), so a naive split would cut a statement in half and the test would report
     * a syntax error rather than the migration's behaviour.
     */
    private static void applyMigration(Connection connection) throws IOException, SQLException {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

        // A control assertion, so a moved or emptied file fails this test instead of
        // passing it: the statements below must be the ones actually shipped.
        assertThat(sql).contains("RENAME COLUMN locked_until TO locked_at");

        for (String statement : splitStatements(stripComments(sql))) {
            execute(connection, statement);
        }
    }

    private static String stripComments(String sql) {
        StringBuilder stripped = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) {
                stripped.append(line).append('\n');
            }
        }
        return stripped.toString();
    }

    /**
     * Statement boundaries, respecting single-quoted literals. A doubled quote
     * inside a literal ({@code administrator''s}) needs no special case: it reads as
     * a close immediately followed by an open, which leaves the parser back inside
     * the literal where it belongs.
     */
    private static List<String> splitStatements(String sql) {
        List<String> statements = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inLiteral = false;

        for (char character : sql.toCharArray()) {
            if (character == '\'') {
                inLiteral = !inLiteral;
            }
            if (character == ';' && !inLiteral) {
                addIfPresent(statements, current);
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        addIfPresent(statements, current);
        return statements;
    }

    private static void addIfPresent(List<String> statements, StringBuilder candidate) {
        if (!candidate.toString().isBlank()) {
            statements.add(candidate.toString());
        }
    }

    private static Object lockedAt(Connection connection, String username) throws SQLException {
        return queryOne(connection, "select locked_at from accounts where username = '"
                + username + "'");
    }

    private static int failedAttempts(Connection connection, String username) throws SQLException {
        return ((Number) queryOne(connection,
                        "select failed_login_attempts from accounts where username = '"
                                + username + "'"))
                .intValue();
    }

    private static List<String> columnNames(Connection connection) throws SQLException {
        List<String> names = new java.util.ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "select attname from pg_attribute"
                                + " where attrelid = 'accounts'::regclass"
                                + " and attnum > 0 and not attisdropped")) {
            while (rows.next()) {
                names.add(rows.getString(1));
            }
        }
        return names;
    }

    private static Object queryOne(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            assertThat(rows.next()).describedAs("the probe row must exist").isTrue();
            return rows.getObject(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
