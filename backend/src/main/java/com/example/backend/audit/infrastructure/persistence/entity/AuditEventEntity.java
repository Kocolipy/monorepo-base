package com.example.backend.audit.infrastructure.persistence.entity;

import com.example.backend.audit.domain.AuditOperation;
import com.example.backend.audit.domain.AuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Database representation of a recorded audit event.
 *
 * <p>Every column is {@code updatable = false}. That is a statement about intent
 * rather than the enforcement — the enforcement is the {@code V3} migration's
 * grants and its {@code BEFORE UPDATE OR DELETE} trigger, which refuse the
 * statement whatever this mapping says. Together they mean an {@code UPDATE} on
 * this table is impossible to reach by accident and impossible to perform on
 * purpose without assuming the retention role.
 */
@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 64)
    private AuditOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private AuditOutcome outcome;

    /** Null when nobody was authenticated, e.g. a rejected login. */
    @Column(updatable = false)
    private UUID actorId;

    /** Null when the operation named no existing account. */
    @Column(updatable = false)
    private UUID subjectId;

    @Column(nullable = false, updatable = false, length = 64)
    private String resourceType;

    @Column(updatable = false)
    private UUID resourceId;

    /**
     * The changed attribute paths, comma-separated. The adapter owns the join and
     * the split; nothing queries an individual path, so the list is stored as one
     * value rather than as a related table.
     */
    @Column(updatable = false)
    private String changedPaths;

    @Column(nullable = false, updatable = false, length = 32)
    private String statusClass;

    @Column(updatable = false, length = 128)
    private String errorCode;

    @Column(updatable = false, length = 16)
    private String httpMethod;

    /** The matched route template. Never the resolved path. */
    @Column(updatable = false, length = 512)
    private String httpPath;

    @Column(updatable = false, length = 128)
    private String requestId;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(
            UUID id,
            Instant occurredAt,
            AuditOperation operation,
            AuditOutcome outcome,
            UUID actorId,
            UUID subjectId,
            String resourceType,
            UUID resourceId,
            String changedPaths,
            String statusClass,
            String errorCode,
            String httpMethod,
            String httpPath,
            String requestId) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.operation = operation;
        this.outcome = outcome;
        this.actorId = actorId;
        this.subjectId = subjectId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.changedPaths = changedPaths;
        this.statusClass = statusClass;
        this.errorCode = errorCode;
        this.httpMethod = httpMethod;
        this.httpPath = httpPath;
        this.requestId = requestId;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public AuditOperation getOperation() {
        return operation;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public UUID getActorId() {
        return actorId;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getChangedPaths() {
        return changedPaths;
    }

    public String getStatusClass() {
        return statusClass;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getHttpPath() {
        return httpPath;
    }

    public String getRequestId() {
        return requestId;
    }
}
