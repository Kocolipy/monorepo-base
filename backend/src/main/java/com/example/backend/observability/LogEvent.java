package com.example.backend.observability;

import java.util.Arrays;
import java.util.List;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * The ECS field names a log record uses to say what happened, and the closed
 * vocabulary their values come from.
 *
 * <p>These are structured fields rather than sentences because that is what makes
 * the redaction rule enforceable: a record's variable part is always a key and a
 * value, and its message is always a constant. Nothing is concatenated into a
 * message, so no value a caller influenced can alter the shape of a record, and a
 * static rule can check the property by looking for concatenation alone — see
 * {@code semgrep/rules/service-security.yml}.
 *
 * <h2>The event vocabulary</h2>
 *
 * <p>{@code event.kind}, {@code event.category}, {@code event.type} and
 * {@code event.action} take their values from the closed enums in the logging
 * standard's {@code Log_Schema.md} §Event, and only the members this service uses
 * are declared here. A record is classified in one call, {@link #classify}, which
 * takes an {@link Operation}: the operations this service logs, each mapped onto
 * the standard's action once, here, rather than at every call site. Nothing else
 * writes {@link #ACTION} or {@link #LOCAL_ACTION} —
 * {@code be-log-event-action-outside-the-vocabulary} holds that — so a free-text
 * action cannot creep back in, and the mapping table in
 * {@code /docs/adr/0003-ecs-structured-logging-with-redaction.md} is the whole of it.
 *
 * <p>Where the standard has no action that fits an operation, or one action covers
 * several of them, the operation's own name is kept under {@link #LOCAL_ACTION}
 * instead of a member being invented for the standard's enum.
 */
public final class LogEvent {

    /** The nature of the record: always {@link Kind#EVENT} here. */
    public static final String KIND = "event.kind";

    /** High-level classification, an array of {@link Category} values. */
    public static final String CATEGORY = "event.category";

    /** Lifecycle within the category, an array of {@link Type} values. */
    public static final String TYPE = "event.type";

    /** What was attempted, as an {@link Action} value. Written by {@link #classify} only. */
    public static final String ACTION = "event.action";

    /**
     * The operation's own name where {@link #ACTION} alone does not identify it — no
     * standard action fits, or one is shared by several operations. Namespaced under
     * {@code app.}, the service's own configuration namespace, so it can never be
     * mistaken for a standard field. Written by {@link #classify} only.
     */
    public static final String LOCAL_ACTION = "app.event.action";

    /** Whether it worked: {@link #SUCCESS} or {@link #FAILURE}. */
    public static final String OUTCOME = "event.outcome";

    /**
     * Why a failure was refused, as a type name from this service's own code. Never
     * a message built from submitted input.
     */
    public static final String REASON = "event.reason";

    /** Operational severity, for routing an alert independently of the level. */
    public static final String SEVERITY = "event.severity";

    /** How long the operation the record ends took, in milliseconds. */
    public static final String DURATION_MS = "event.duration_ms";

    /**
     * The stable SCIM id of the User a record's operation was performed ON, when that
     * is a different role from the actor. The actor is {@code user.id}, which
     * {@link LogContext} carries for the whole request; ECS names the acted-on
     * identity {@code user.target.*}. Never a userName.
     */
    public static final String USER_TARGET_ID = "user.target.id";

    public static final String SUCCESS = "success";

    public static final String FAILURE = "failure";

    /**
     * Which audit operation a record is about, as an
     * {@link com.example.backend.audit.domain.AuditOperation} name. Carried by the
     * operational alert raised when an event could not be appended, so the alert
     * says which record is missing from the trail.
     */
    public static final String AUDIT_OPERATION = "audit.operation";

    /** The configured audit retention window, as an ISO-8601 duration. */
    public static final String RETENTION_PERIOD = "audit.retention.period";

    /** How many aged-out events one retention run removed. */
    public static final String RETENTION_DELETED_ROWS = "audit.retention.deleted_rows";

    /** A dormancy job's configured window, as an ISO-8601 duration. */
    public static final String DORMANCY_WINDOW = "dormancy.window";

    /** How many Users one dormancy run changed. */
    public static final String DORMANCY_PROCESSED = "dormancy.processed";

    /**
     * The cron expression a scheduled job runs on, on its startup record.
     * {@code Log_Schema.md} §Trigger.
     */
    public static final String TRIGGER_CRON_EXPRESSION = "trigger.cron.expression";

    /** The IANA zone that cron is evaluated in. {@code Log_Schema.md} §Trigger. */
    public static final String TRIGGER_CRON_TIMEZONE = "trigger.cron.timezone";

    /**
     * What a scheduled job does, in a sentence an operator reads on its startup record. The
     * schema has no field for it, so it is namespaced under {@code app.}, as
     * {@link #LOCAL_ACTION} is.
     */
    public static final String JOB_DESCRIPTION = "app.job.description";

    /**
     * {@code Log_Schema.md} §Error {@code error.code}, spelled with an underscore: Boot's ECS
     * formatter owns the {@code error} object (it writes {@code error.type},
     * {@code error.message} and {@code error.stack_trace} from the attached throwable), so
     * a dotted key of ours inside it would collide with that object. The standard's own
     * recipes use this spelling for the same reason.
     */
    public static final String ERROR_CODE = "error_code";

    /** {@code Log_Schema.md} §Error {@code error.category}, an {@link ErrorCategory}; see {@link #ERROR_CODE}. */
    public static final String ERROR_CATEGORY = "error_category";

    /**
     * {@code Log_Schema.md} §Error {@code error.follow_up_action}: whether the error needs a
     * person to act on it. See {@link #ERROR_CODE} for the spelling.
     */
    public static final String ERROR_FOLLOW_UP_ACTION = "error_follow_up_action";

    /**
     * The {@link #REASON} of a scheduled run that found another run of the same job holding
     * the job's lock, and so did nothing.
     */
    public static final String REASON_LOCK_HELD = "lock-held";

    /** An inbound request's method, from a closed set. ECS {@code http.request.method}. */
    public static final String HTTP_METHOD = "http.request.method";

    /**
     * The route TEMPLATE an inbound request matched ({@code /scim/v2/Users/{id}}), never the
     * path it arrived on, so no id or filter text a caller put in the URL reaches a record.
     * Neither ECS nor {@code Log_Schema.md} names a template field; this is OpenTelemetry's
     * {@code http.route}.
     */
    public static final String HTTP_ROUTE = "http.route";

    /** The status an inbound request was answered with. ECS {@code http.response.status_code}. */
    public static final String HTTP_STATUS_CODE = "http.response.status_code";

    /**
     * The SCIM resource type a refused request addressed ({@code User}, {@code Group}), read
     * from the route it matched. Never anything from the request body.
     */
    public static final String SCIM_RESOURCE_TYPE = "scim.resource.type";

    /** How many sessions one revocation ended. */
    public static final String SESSIONS_ENDED = "session.ended_count";

    /** The machine's host name, on the startup record. ECS {@code host.name}. */
    public static final String HOST_NAME = "host.name";

    /** The machine's own address — never a client's — on the startup record. ECS {@code host.ip}. */
    public static final String HOST_IP = "host.ip";

    private LogEvent() {
    }

    /**
     * Classifies a record: {@code event.kind}, {@code event.category},
     * {@code event.type}, and the operation's {@code event.action} and local name
     * where it has them.
     *
     * @return {@code record}, for the rest of the fluent chain
     */
    public static LoggingEventBuilder classify(
            LoggingEventBuilder record, Operation operation, Category category, Type... types) {
        LoggingEventBuilder classified = record
                .addKeyValue(KIND, Kind.EVENT.value())
                .addKeyValue(CATEGORY, List.of(category.value()))
                .addKeyValue(TYPE, Arrays.stream(types).map(Type::value).toList());
        if (operation.action() != null) {
            classified = classified.addKeyValue(ACTION, operation.action().value());
        }
        if (operation.local() != null) {
            classified = classified.addKeyValue(LOCAL_ACTION, operation.local());
        }
        return classified;
    }

    /**
     * What this service logs, each mapped onto the standard's {@link Action} — or onto
     * none, where none fits — and carrying its own name wherever that action alone
     * would not say which operation it was.
     */
    public enum Operation {
        LOGIN(Action.USER_AUTHENTICATION, null),
        UNLOCK(Action.ACCESS_CONTROL, "identity.unlock"),
        FORCE_PASSWORD_CHANGE(Action.PASSWORD_CHANGE_ENFORCEMENT, null),
        PASSWORD_CHANGE(Action.USER_ADMINISTRATION, "identity.password_change"),
        INACTIVITY_DEACTIVATION(Action.USER_ADMINISTRATION, "identity.inactivity_deactivation"),
        DORMANT_AUTHORITY_REVOCATION(
                Action.ACCESS_CONTROL, "identity.dormant_authority_revocation"),
        CONNECTOR_CREATE(Action.ACCESS_CONTROL, "scim.connector.create"),
        CONNECTOR_DELETE(Action.ACCESS_CONTROL, "scim.connector.delete"),
        CONNECTOR_TOKEN_ISSUE(Action.ACCESS_CONTROL, "scim.connector.token.issue"),
        CONNECTOR_TOKEN_ROTATE(Action.ACCESS_CONTROL, "scim.connector.token.rotate"),
        CONNECTOR_TOKEN_REVOKE(Action.ACCESS_CONTROL, "scim.connector.token.revoke"),
        SCIM_WRITE(Action.USER_PROVISIONING, "scim.write"),
        SCIM_REFUSAL(Action.USER_PROVISIONING, "scim.refusal"),
        ACCESS_DENIED(Action.ACCESS_CONTROL, "access.denied"),
        UNAUTHENTICATED(Action.ACCESS_CONTROL, "access.unauthenticated"),
        LOGOUT(Action.USER_LOGOUT, null),
        SESSION_END(Action.SESSION_END, null),
        AUDIT_RETENTION(null, "audit.retention"),
        AUDIT_APPEND(null, "audit.append"),
        HTTP_REQUEST(null, "http.request"),
        APPLICATION_STARTUP(Action.APPLICATION_STARTUP, null),
        APPLICATION_SHUTDOWN(Action.APPLICATION_SHUTDOWN, null);

        private final Action action;
        private final String local;

        Operation(Action action, String local) {
            this.action = action;
            this.local = local;
        }

        /** The standard's action, or {@code null} where none fits. */
        public Action action() {
            return action;
        }

        /** The operation's own name, or {@code null} where {@link #action()} identifies it. */
        public String local() {
            return local;
        }
    }

    /** {@code event.action} values from {@code Log_Schema.md} §Event that this service uses. */
    public enum Action {
        USER_AUTHENTICATION("user-authentication"),
        USER_LOGOUT("user-logout"),
        SESSION_END("session-end"),
        USER_ADMINISTRATION("user-administration"),
        USER_PROVISIONING("user-provisioning"),
        PASSWORD_CHANGE_ENFORCEMENT("password-change-enforcement"),
        ACCESS_CONTROL("access-control"),
        APPLICATION_STARTUP("application-startup"),
        APPLICATION_SHUTDOWN("application-shutdown");

        private final String value;

        Action(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** {@code event.kind} values from {@code Log_Schema.md} §Event that this service uses. */
    public enum Kind {
        EVENT("event");

        private final String value;

        Kind(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** {@code event.category} values from {@code Log_Schema.md} §Event that this service uses. */
    public enum Category {
        CONFIGURATION("configuration"),
        DATABASE("database"),
        BATCH("batch"),
        NETWORK("network"),
        PROCESS("process");

        private final String value;

        Category(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** {@code event.type} values from {@code Log_Schema.md} §Event that this service uses. */
    public enum Type {
        ACCESS("access"),
        ADMIN("admin"),
        ALLOWED("allowed"),
        CHANGE("change"),
        CREATION("creation"),
        DELETION("deletion"),
        DENIED("denied"),
        END("end"),
        ERROR("error"),
        INFO("info"),
        JOB_END("job-end"),
        JOB_START("job-start"),
        START("start"),
        USER("user");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** {@code event.severity} values from {@code Log_Schema.md} §Event that this service uses. */
    public enum Severity {
        LOW("low"),
        HIGH("high");

        private final String value;

        Severity(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** {@code error.category} values from {@code Log_Schema.md} §Error that this service uses. */
    public enum ErrorCategory {
        APPLICATION("application"),
        DATABASE("database");

        private final String value;

        ErrorCategory(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
