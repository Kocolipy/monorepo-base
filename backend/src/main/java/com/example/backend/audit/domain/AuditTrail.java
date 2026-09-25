package com.example.backend.audit.domain;

import java.util.UUID;

/**
 * The one way anything in this service records that something happened.
 *
 * <p><strong>No method here takes a {@link String}.</strong> That is the redaction
 * rule expressed as a signature rather than as a review note: a username, a
 * password and a bearer value are all strings, so a boundary that admits none
 * cannot be handed one by a caller who never read the rule. Every reference to a
 * person or a resource is a stable id, and every classification is a member of a
 * closed set. {@code ArchitectureTest.the_audit_trail_boundary_admits_no_free_text}
 * holds the shape, and {@code semgrep/rules/service-security.yml} flags a call
 * that tries to pass a credential-named value through it.
 *
 * <p>Which appends are fail-closed and which are fail-open with an operational
 * alert is documented on the implementation and in
 * {@code /docs/adr/0004-audit-append-failure-semantics.md}. It is stated there
 * rather than here because it is a property of how the recording is arranged, not
 * of what a caller asks for: a caller says what happened and does not choose
 * whether the trail may fail silently.
 */
public interface AuditTrail {

    /** Records an accepted login. */
    void recordLoginSuccess(UUID accountId);

    /**
     * Records a refused login.
     *
     * @param subjectId stable id of the account the attempt named, or {@code null}
     *                  when the submitted username names no account — the one thing
     *                  that must not be recorded in its place
     */
    void recordLoginFailure(UUID subjectId, AuditRefusalReason reason);

    /** Records a session ended by its holder. */
    void recordLogout(UUID accountId);

    /** Records a failure run reaching the configured limit. */
    void recordLockoutSet(UUID accountId);

    /** Records a lockout that ran out, observed at the next attempt. */
    void recordLockoutLiftedByExpiry(UUID accountId);

    /** Records an administrator ending a lockout early. */
    void recordLockoutLiftedByUnlock(UUID actorId, UUID subjectId);

    /** Records an account closed to logins. */
    void recordAccountDisabled(UUID actorId, UUID subjectId);

    /** Records an account reopened to logins. */
    void recordAccountEnabled(UUID actorId, UUID subjectId);

    /**
     * Records a connector created.
     *
     * <p>Every method below identifies the event by the CONNECTOR's id and never by
     * the token's, and none of them can be handed a token value: the plaintext is a
     * {@code String}, and this boundary declares none. That is the "no plaintext
     * token in an event" requirement expressed the same way the "no userName in an
     * event" requirement is — as a signature rather than as a review note.
     */
    void recordConnectorCreated(UUID actorId, UUID connectorId);

    /**
     * Records a connector deleted, along with the tokens and aliases that went with
     * it.
     */
    void recordConnectorDeleted(UUID actorId, UUID connectorId);

    /** Records a token minted for a connector. */
    void recordConnectorTokenIssued(UUID actorId, UUID connectorId);

    /** Records a connector's token replaced, the old one ending at the overlap. */
    void recordConnectorTokenRotated(UUID actorId, UUID connectorId);

    /**
     * Records a connector token revoked.
     *
     * @param actorId the administrator who revoked it, or {@code null} when the
     *                revocation was part of deleting the connector — that event
     *                carries the actor, and repeating it here would suggest two
     *                separate administrative acts
     */
    void recordConnectorTokenRevoked(UUID actorId, UUID connectorId);
}
