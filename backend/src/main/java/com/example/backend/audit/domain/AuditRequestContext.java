package com.example.backend.audit.domain;

/**
 * Port for "which request am I recording this under".
 *
 * <p>A port rather than a servlet lookup inside the application service, for the
 * same reason session revocation is one: the application layer states what it
 * needs — the method, the route template and the correlation id — and an adapter
 * owns how the surrounding runtime is asked. It also means the one place that
 * knows a resolved path must never be recorded is a single adapter a test can
 * point at, instead of every call site that builds an event.
 */
public interface AuditRequestContext {

    /**
     * The request currently being served, or {@link AuditRequest#NONE} when this
     * thread is not serving one.
     */
    AuditRequest current();
}
