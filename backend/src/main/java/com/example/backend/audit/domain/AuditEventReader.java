package com.example.backend.audit.domain;

/**
 * Read port for the audit trail: the administrative listing, and nothing else.
 *
 * <p>Separate from {@link AuditEventRepository} so that the port every recording path depends on
 * stays append-only in shape. A reader and a writer have no caller in common: the recording paths
 * never read an event back, and the listing never writes one — so the listing adds no event type
 * and records nothing about itself.
 */
public interface AuditEventReader {

    /** The page of matching events the query asks for, newest first. */
    AuditEventPage find(AuditEventQuery query);
}
