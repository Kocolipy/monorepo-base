package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.backend.audit.CapturedLog;
import com.example.backend.observability.LogEvent.Action;
import com.example.backend.observability.LogEvent.Category;
import com.example.backend.observability.LogEvent.ErrorCategory;
import com.example.backend.observability.LogEvent.Kind;
import com.example.backend.observability.LogEvent.Operation;
import com.example.backend.observability.LogEvent.Severity;
import com.example.backend.observability.LogEvent.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The event vocabulary: every value it can write is a member of the logging
 * standard's closed enum ({@code Log_Schema.md} §Event), and {@link LogEvent#classify}
 * writes exactly the fields an operation's mapping says it should.
 */
class LogEventTests {

    private static final Logger log = LoggerFactory.getLogger(LogEventTests.class);

    /** {@code Log_Schema.md} §Event, the allowed values this service may draw from. */
    private static final Set<String> STANDARD_ACTIONS = Set.of(
            "user-authentication", "user-logout", "user-provisioning", "user-administration",
            "password-reset", "password-change-enforcement", "session-start", "session-end",
            "access-control", "application-startup", "application-shutdown");
    private static final Set<String> STANDARD_KINDS = Set.of("event", "state");
    private static final Set<String> STANDARD_CATEGORIES = Set.of(
            "configuration", "network", "database", "batch", "interface", "process");
    private static final Set<String> STANDARD_TYPES = Set.of(
            "access", "admin", "allowed", "change", "connection", "creation", "deletion",
            "denied", "end", "error", "group", "indicator", "info", "installation",
            "interface-end", "interface-start", "job-end", "job-start", "protocol", "start",
            "step-end", "step-start", "user");
    private static final Set<String> STANDARD_SEVERITIES =
            Set.of("low", "medium", "high", "critical");
    /** {@code Log_Schema.md} §Error {@code error.category}. */
    private static final Set<String> STANDARD_ERROR_CATEGORIES = Set.of(
            "server", "network", "cert/auth", "database", "application", "data", "others");

    /**
     * Where ADR 0003 records each operation's exception to the schema's required
     * {@code event.action}, with the reason no allowed value fits.
     */
    private static final String NO_ACTION_SECTION =
            "docs/adr/0003 § Addendum (2026-10-02): user.id on the request record, and every"
                    + " record's event.action — Operations with no event.action";

    /**
     * The operations that carry no {@code event.action}, each naming the ADR section that
     * says why. An operation added with no action and no entry here fails
     * {@link #everyOperationHasAnActionOrADocumentedException}; so does an entry left
     * behind after its operation gains one.
     */
    private static final Map<Operation, String> NO_ACTION_EXCEPTIONS = Map.of(
            Operation.HTTP_REQUEST, NO_ACTION_SECTION,
            Operation.HTTP_REQUEST_REFUSAL, NO_ACTION_SECTION,
            Operation.HTTP_REQUEST_FAULT, NO_ACTION_SECTION,
            Operation.AUDIT_RETENTION, NO_ACTION_SECTION,
            Operation.AUDIT_APPEND, NO_ACTION_SECTION);

    /**
     * Every operation either maps onto a standard action or is a documented exception —
     * never both, and never neither. An exception still names its operation, by
     * {@code app.event.action}.
     */
    @ParameterizedTest
    @EnumSource(Operation.class)
    void everyOperationHasAnActionOrADocumentedException(Operation operation) {
        if (NO_ACTION_EXCEPTIONS.containsKey(operation)) {
            assertThat(operation.action())
                    .as("%s is listed as an exception but has an action", operation)
                    .isNull();
            assertThat(NO_ACTION_EXCEPTIONS.get(operation)).startsWith("docs/adr/0003 § ");
            assertThat(operation.local()).isNotBlank();
        } else {
            assertThat(operation.action())
                    .as("%s has no event.action and no documented exception", operation)
                    .isNotNull();
            assertThat(STANDARD_ACTIONS).contains(operation.action().value());
        }
    }

    /**
     * The mapping #95 changed: dormant-authority revocation is administration of a User's
     * standing, and a connector and its tokens are the provisioning channel's lifecycle —
     * except issuing and rotating a token, which grant Permissions and so are user
     * administration (#117, the spec's observability table). {@code access-control} is left to
     * the access decisions themselves.
     */
    @Test
    void eachOperationCarriesItsNearestStandardAction() {
        assertThat(Operation.DORMANT_AUTHORITY_REVOCATION.action())
                .isEqualTo(Action.USER_ADMINISTRATION);
        assertThat(List.of(Operation.CONNECTOR_CREATE, Operation.CONNECTOR_DELETE,
                        Operation.CONNECTOR_TOKEN_REVOKE))
                .extracting(Operation::action)
                .containsOnly(Action.USER_PROVISIONING);
        assertThat(List.of(Operation.CONNECTOR_TOKEN_ISSUE, Operation.CONNECTOR_TOKEN_ROTATE))
                .extracting(Operation::action)
                .containsOnly(Action.USER_ADMINISTRATION);
        assertThat(Arrays.stream(Operation.values())
                        .filter(operation -> operation.action() == Action.ACCESS_CONTROL))
                .containsExactlyInAnyOrder(
                        Operation.UNLOCK, Operation.ACCESS_DENIED, Operation.UNAUTHENTICATED);
    }

    @Test
    void everyDeclaredValueIsAMemberOfTheStandardsEnum() {
        assertThat(values(Action.values(), Action::value)).isSubsetOf(STANDARD_ACTIONS);
        assertThat(values(Kind.values(), Kind::value)).isSubsetOf(STANDARD_KINDS);
        assertThat(values(Category.values(), Category::value)).isSubsetOf(STANDARD_CATEGORIES);
        assertThat(values(Type.values(), Type::value)).isSubsetOf(STANDARD_TYPES);
        assertThat(values(Severity.values(), Severity::value)).isSubsetOf(STANDARD_SEVERITIES);
        assertThat(values(ErrorCategory.values(), ErrorCategory::value))
                .isSubsetOf(STANDARD_ERROR_CATEGORIES);
    }

    /** Pins each spelling, so a typo in one constant is a failure rather than a subset. */
    @Test
    void theDeclaredSpellingsAreTheStandards() {
        assertThat(Action.USER_AUTHENTICATION.value()).isEqualTo("user-authentication");
        assertThat(Action.USER_LOGOUT.value()).isEqualTo("user-logout");
        assertThat(Action.SESSION_START.value()).isEqualTo("session-start");
        assertThat(Action.SESSION_END.value()).isEqualTo("session-end");
        assertThat(Action.USER_ADMINISTRATION.value()).isEqualTo("user-administration");
        assertThat(Action.USER_PROVISIONING.value()).isEqualTo("user-provisioning");
        assertThat(Action.PASSWORD_CHANGE_ENFORCEMENT.value())
                .isEqualTo("password-change-enforcement");
        assertThat(Action.ACCESS_CONTROL.value()).isEqualTo("access-control");
        assertThat(Action.APPLICATION_STARTUP.value()).isEqualTo("application-startup");
        assertThat(Action.APPLICATION_SHUTDOWN.value()).isEqualTo("application-shutdown");
        assertThat(Kind.EVENT.value()).isEqualTo("event");
        assertThat(Category.CONFIGURATION.value()).isEqualTo("configuration");
        assertThat(Category.DATABASE.value()).isEqualTo("database");
        assertThat(Category.BATCH.value()).isEqualTo("batch");
        assertThat(Category.NETWORK.value()).isEqualTo("network");
        assertThat(Category.PROCESS.value()).isEqualTo("process");
        assertThat(Type.ACCESS.value()).isEqualTo("access");
        assertThat(Type.START.value()).isEqualTo("start");
        assertThat(Type.END.value()).isEqualTo("end");
        assertThat(Type.JOB_END.value()).isEqualTo("job-end");
        assertThat(Type.JOB_START.value()).isEqualTo("job-start");
        assertThat(Severity.LOW.value()).isEqualTo("low");
        assertThat(Severity.HIGH.value()).isEqualTo("high");
        assertThat(ErrorCategory.APPLICATION.value()).isEqualTo("application");
        assertThat(ErrorCategory.DATABASE.value()).isEqualTo("database");
    }

    /**
     * An operation is identifiable from its record: by its local name where it has
     * one, or else by an action no other operation WITHOUT a local name shares — a
     * record carrying no {@code app.event.action} is then the one operation with that
     * action and no local name, however many named operations share the action.
     */
    @Test
    void everyOperationIsIdentifiableFromItsRecord() {
        Map<Action, Long> sharing = Arrays.stream(Operation.values())
                .filter(operation -> operation.action() != null && operation.local() == null)
                .collect(Collectors.groupingBy(Operation::action, Collectors.counting()));

        assertThat(Operation.values()).allSatisfy(operation -> {
            if (operation.local() == null) {
                assertThat(operation.action()).isNotNull();
                assertThat(sharing.get(operation.action())).isEqualTo(1L);
            }
        });
        assertThat(Arrays.stream(Operation.values()).map(Operation::local).filter(l -> l != null))
                .doesNotHaveDuplicates();
    }

    @Test
    void classifyWritesTheStandardFieldsAndTheLocalName() {
        Map<String, Object> fields = classified(
                Operation.UNLOCK, Category.PROCESS, Type.ADMIN, Type.USER, Type.CHANGE);

        assertThat(fields)
                .containsEntry(LogEvent.KIND, "event")
                .containsEntry(LogEvent.CATEGORY, List.of("process"))
                .containsEntry(LogEvent.TYPE, List.of("admin", "user", "change"))
                .containsEntry(LogEvent.ACTION, "access-control")
                .containsEntry(LogEvent.LOCAL_ACTION, "identity.unlock");
    }

    @Test
    void anOperationWithAnExactActionWritesNoLocalName() {
        Map<String, Object> fields =
                classified(Operation.LOGIN, Category.PROCESS, Type.USER, Type.ALLOWED);

        assertThat(fields)
                .containsEntry(LogEvent.ACTION, "user-authentication")
                .doesNotContainKey(LogEvent.LOCAL_ACTION);
    }

    @Test
    void anOperationNoActionFitsWritesItsLocalNameAlone() {
        Map<String, Object> fields =
                classified(Operation.AUDIT_RETENTION, Category.BATCH, Type.JOB_END);

        assertThat(fields)
                .containsEntry(LogEvent.LOCAL_ACTION, "audit.retention")
                .containsEntry(LogEvent.TYPE, List.of("job-end"))
                .doesNotContainKey(LogEvent.ACTION);
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void everyOperationClassifiesToItsMapping(Operation operation) {
        Map<String, Object> fields = classified(operation, Category.PROCESS, Type.INFO);

        assertThat(fields.get(LogEvent.ACTION))
                .isEqualTo(operation.action() == null ? null : operation.action().value());
        assertThat(fields.get(LogEvent.LOCAL_ACTION)).isEqualTo(operation.local());
    }

    /** {@code atError} opens an {@code ERROR} record already carrying all three error fields. */
    @Test
    void atErrorOpensAnErrorRecordCarryingTheErrorClassification() {
        try (CapturedLog captured = CapturedLog.attach()) {
            LogEvent.classify(LogEvent.atError(log, 503, ErrorCategory.DATABASE, true),
                            Operation.AUDIT_APPEND, Category.DATABASE, Type.ERROR)
                    .log("failed");

            List<ILoggingEvent> records = captured.withAction(Level.TRACE, LogEvent.KIND, "event");
            assertThat(records).hasSize(1);
            assertThat(records.getFirst().getLevel()).isEqualTo(Level.ERROR);
            assertThat(CapturedLog.fields(records.getFirst()))
                    .containsEntry(LogEvent.ERROR_CODE, 503)
                    .containsEntry(LogEvent.ERROR_CATEGORY, "database")
                    .containsEntry(LogEvent.ERROR_FOLLOW_UP_ACTION, true);
        }
    }

    /** {@code withError} classifies a record below {@code ERROR} and leaves its level alone. */
    @Test
    void withErrorClassifiesARecordAtItsOwnLevel() {
        try (CapturedLog captured = CapturedLog.attach()) {
            LogEvent.classify(LogEvent.withError(log.atWarn(), 400, ErrorCategory.DATA, false),
                            Operation.HTTP_REQUEST_REFUSAL, Category.PROCESS, Type.DENIED)
                    .log("refused");

            List<ILoggingEvent> records = captured.withAction(Level.TRACE, LogEvent.KIND, "event");
            assertThat(records).hasSize(1);
            assertThat(records.getFirst().getLevel()).isEqualTo(Level.WARN);
            assertThat(CapturedLog.fields(records.getFirst()))
                    .containsEntry(LogEvent.ERROR_CODE, 400)
                    .containsEntry(LogEvent.ERROR_CATEGORY, "data")
                    .containsEntry(LogEvent.ERROR_FOLLOW_UP_ACTION, false)
                    .containsEntry(LogEvent.LOCAL_ACTION, "http.request.refusal");
        }
    }

    /** The call-site spellings the encoder's customizer moves into {@code error}. */
    @Test
    void theErrorKeysAreTheUnderscoreSpellings() {
        assertThat(LogEvent.ERROR_CODE).isEqualTo("error_code");
        assertThat(LogEvent.ERROR_CATEGORY).isEqualTo("error_category");
        assertThat(LogEvent.ERROR_FOLLOW_UP_ACTION).isEqualTo("error_follow_up_action");
        assertThat(LogEvent.ERROR_CAUSE_OMITTED).isEqualTo("app.error.cause_omitted");
        assertThat(ErrorCategory.DATA.value()).isEqualTo("data");
    }

    /** {@code Log_Schema.md} has no session field; the AuthN recipe's spelling is the one used. */
    @Test
    void theSessionStartFieldIsTheRecipesSpelling() {
        assertThat(LogEvent.SESSION_MAX_INACTIVE_INTERVAL)
                .isEqualTo("session.max_inactive_interval");
    }

    private static Map<String, Object> classified(
            Operation operation, Category category, Type... types) {
        try (CapturedLog captured = CapturedLog.attach()) {
            LogEvent.classify(log.atWarn(), operation, category, types).log("classified");
            List<ILoggingEvent> records = captured.withAction(Level.WARN, LogEvent.KIND, "event");
            assertThat(records).hasSize(1);
            return CapturedLog.fields(records.getFirst());
        }
    }

    private static <E> Set<String> values(E[] members, java.util.function.Function<E, String> value) {
        return Arrays.stream(members).map(value).collect(Collectors.toSet());
    }
}
