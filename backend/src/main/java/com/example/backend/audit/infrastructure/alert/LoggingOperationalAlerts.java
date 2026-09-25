package com.example.backend.audit.infrastructure.alert;

import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.OperationalAlerts;
import com.example.backend.observability.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Raises an operational alert as an {@code ERROR} record in the structured log
 * stream, which is what a collector alerts on.
 *
 * <p>{@code ERROR} is the level and not {@code WARN} on purpose: an audit event
 * that could not be appended means the trail has a hole in it, and the request it
 * belonged to completed anyway. Nobody learns that from anywhere else, so it is
 * not a condition to notice in aggregate — it is one to be told about.
 *
 * <p>Nothing variable reaches the message. Both values are names from closed sets
 * — the operation, and the failure's own class — emitted as structured fields, so
 * the alert cannot become the thing that leaks the value the unwritten event was
 * careful not to record.
 */
@Component
class LoggingOperationalAlerts implements OperationalAlerts {

    private static final Logger log = LoggerFactory.getLogger(LoggingOperationalAlerts.class);

    /** Shared {@code event.action} so one search finds every append failure. */
    static final String AUDIT_APPEND_ACTION = "audit.append";

    @Override
    public void auditAppendFailed(AuditOperation operation, Class<? extends Throwable> failure) {
        log.atError()
                .addKeyValue(LogEvent.ACTION, AUDIT_APPEND_ACTION)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.FAILURE)
                .addKeyValue(LogEvent.REASON, failure.getSimpleName())
                .addKeyValue(LogEvent.AUDIT_OPERATION, operation.name())
                .log("Audit event could not be appended; the request was not altered");
    }
}
