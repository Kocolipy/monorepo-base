package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.backend.audit.CapturedLog;
import com.example.backend.observability.LogEvent.Action;
import com.example.backend.observability.LogEvent.Category;
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

    @Test
    void everyDeclaredValueIsAMemberOfTheStandardsEnum() {
        assertThat(values(Action.values(), Action::value)).isSubsetOf(STANDARD_ACTIONS);
        assertThat(values(Kind.values(), Kind::value)).isSubsetOf(STANDARD_KINDS);
        assertThat(values(Category.values(), Category::value)).isSubsetOf(STANDARD_CATEGORIES);
        assertThat(values(Type.values(), Type::value)).isSubsetOf(STANDARD_TYPES);
        assertThat(values(Severity.values(), Severity::value)).isSubsetOf(STANDARD_SEVERITIES);
    }

    /** Pins each spelling, so a typo in one constant is a failure rather than a subset. */
    @Test
    void theDeclaredSpellingsAreTheStandards() {
        assertThat(Action.USER_AUTHENTICATION.value()).isEqualTo("user-authentication");
        assertThat(Action.USER_LOGOUT.value()).isEqualTo("user-logout");
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
        assertThat(Severity.LOW.value()).isEqualTo("low");
        assertThat(Severity.HIGH.value()).isEqualTo("high");
    }

    /**
     * An operation is identifiable from its record: by its local name where it has
     * one, or else by an action no other operation shares.
     */
    @Test
    void everyOperationIsIdentifiableFromItsRecord() {
        Map<Action, Long> sharing = Arrays.stream(Operation.values())
                .filter(operation -> operation.action() != null)
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
