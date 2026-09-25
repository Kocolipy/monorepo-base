package com.example.backend.audit.domain;

/**
 * Whether the audited operation worked. Not whether recording it worked — a
 * recorded event is by definition one that was appended.
 */
public enum AuditOutcome {

    SUCCESS,

    FAILURE
}
