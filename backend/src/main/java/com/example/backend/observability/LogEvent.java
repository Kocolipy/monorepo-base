package com.example.backend.observability;

/**
 * The ECS field names a log record uses to say what happened, and the two
 * outcomes it can report.
 *
 * <p>These are structured fields rather than sentences because that is what makes
 * the redaction rule enforceable: a record's variable part is always a key and a
 * value, and its message is always a constant. Nothing is concatenated into a
 * message, so no value a caller influenced can alter the shape of a record, and a
 * static rule can check the property by looking for concatenation alone — see
 * {@code semgrep/rules/service-security.yml}.
 *
 * <p>Held in one place so the two call sites that emit account events cannot drift
 * into logging {@code event.action} under two spellings, which would make a log
 * search silently incomplete.
 */
public final class LogEvent {

    /** What was attempted, e.g. {@code login} or {@code account.disable}. */
    public static final String ACTION = "event.action";

    /** Whether it worked: {@link #SUCCESS} or {@link #FAILURE}. */
    public static final String OUTCOME = "event.outcome";

    /**
     * Why a failure was refused, as a type name from this service's own code. Never
     * a message built from submitted input.
     */
    public static final String REASON = "event.reason";

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

    /** The cron expression the retention job runs on. */
    public static final String RETENTION_SCHEDULE = "audit.retention.schedule";

    /** How many aged-out events one retention run removed. */
    public static final String RETENTION_DELETED_ROWS = "audit.retention.deleted_rows";

    /** How long one retention run took, in milliseconds. */
    public static final String RETENTION_DURATION_MS = "audit.retention.duration_ms";

    private LogEvent() {
    }
}
