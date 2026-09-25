package com.example.backend.audit.domain;

/**
 * Raising an operator's attention, for the one case the audit trail cannot report
 * through itself: an append that failed.
 *
 * <p>A port rather than a log call inside the application service because "an
 * alert was raised" is a claim a test has to be able to check without reading log
 * bytes, and because where an alert goes is a deployment decision — a log record
 * a collector alerts on today, a metric or a pager tomorrow — while whether one is
 * raised is a rule of the use case.
 *
 * <p>Nothing here takes a message. An alert names an operation and the type of
 * failure, both from closed sets, so an alert can never be the thing that leaks
 * the value an audit event was careful not to record.
 */
public interface OperationalAlerts {

    /**
     * Reports that an audit event could not be appended, and that the request it
     * belonged to was allowed to finish anyway.
     *
     * @param operation what the unrecorded event was going to say happened
     * @param failure   the failure's own type
     */
    void auditAppendFailed(AuditOperation operation, Class<? extends Throwable> failure);
}
