package com.example.backend.audit.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One recorded thing that happened, as the audit trail holds it.
 *
 * <p>Every reference to a person or a resource is a {@link UUID} — the account's
 * stable, non-reassignable id — and there is deliberately no field a username, a
 * password or a bearer value could be written into. That is the redaction rule
 * expressed as a shape rather than as a review note: {@code actorId} and
 * {@code subjectId} are ids because the readable identifiers are exactly what
 * must not be retained, and the remaining {@code String} fields carry values from
 * closed vocabularies the audit slice owns (a status class, an error code that is
 * a reason name, a route template) rather than anything a caller submitted.
 *
 * <p>An event is never updated and never deleted except by retention. Nothing
 * here offers a {@code with...} transition for that reason: correcting a recorded
 * event is not a capability, and the database refuses the statement that would.
 *
 * @param id            the event's own identity, assigned before the insert
 * @param occurredAt    when the audited operation happened
 * @param operation     what happened
 * @param outcome       whether the operation worked
 * @param actorId       stable id of the account that acted, or {@code null} when
 *                      nobody was authenticated
 * @param subjectId     stable id of the account acted on, or {@code null} when
 *                      the operation named no existing account
 * @param resourceType  the kind of resource acted on
 * @param resourceId    stable id of the resource acted on, normally the same as
 *                      {@code subjectId} while accounts are the only resource
 * @param changedPaths  attribute paths the operation changed, from a fixed
 *                      vocabulary; empty when it changed nothing
 * @param statusClass   how the triggering request ended, classified
 * @param errorCode     a reason name from this service's own code, or
 *                      {@code null}; never a message
 * @param httpMethod    method of the triggering request, or {@code null} when it
 *                      was not one
 * @param httpPath      matched route TEMPLATE of the triggering request, never
 *                      the resolved path — that carries the username in it
 * @param requestId     correlation id minted for the triggering request
 */
public record AuditEvent(
        UUID id,
        Instant occurredAt,
        AuditOperation operation,
        AuditOutcome outcome,
        UUID actorId,
        UUID subjectId,
        String resourceType,
        UUID resourceId,
        List<String> changedPaths,
        String statusClass,
        String errorCode,
        String httpMethod,
        String httpPath,
        String requestId) {

    /** The login identity, as the audit trail names it. */
    public static final String ACCOUNT_RESOURCE_TYPE = "Account";

    /**
     * A SCIM connector, as the audit trail names it.
     *
     * <p>A token lifecycle event carries this type and the CONNECTOR's id, not the
     * token's. The connector is what an administrator investigates and what
     * survives a rotation; a token id identifies a credential that may already be
     * gone, and grouping a connector's history by it would split one integration's
     * story across every token it ever held. Which token an event is about is the
     * operation plus the timestamp, on a stream that is append-only.
     */
    public static final String CONNECTOR_RESOURCE_TYPE = "ScimConnector";

    /** The request succeeded. */
    public static final String STATUS_OK = "ok";

    /** The request was refused because of what it was or who made it. */
    public static final String STATUS_CLIENT_ERROR = "client_error";

    /** The request failed inside this service. */
    public static final String STATUS_SERVER_ERROR = "server_error";

    public AuditEvent {
        changedPaths = changedPaths == null ? List.of() : List.copyOf(changedPaths);
    }
}
