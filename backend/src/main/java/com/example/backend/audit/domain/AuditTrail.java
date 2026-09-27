package com.example.backend.audit.domain;

import java.util.Set;
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

    /**
     * Records an administrator ending a lockout.
     *
     * <p>The only lift there is, which is why it is the only one declared: a lock
     * does not expire, so there is no unrequested lift to record and no code path
     * that could record one. {@code actorId} is required by the signature for the
     * same reason — a lift that named no administrator would be describing
     * something this application cannot do.
     */
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

    /**
     * Records a connector creating a SCIM User.
     *
     * @param connectorId the acting connector
     * @param userId      the created resource's stable id
     */
    void recordScimUserCreated(UUID connectorId, UUID userId);

    /**
     * Records a create refused because a live User already holds the
     * {@code userName}.
     *
     * <p>No subject id, because there is none: the resource was not created, and the
     * existing User that holds the name is not what the event is about. Naming it
     * would turn a refused create into an event against an unrelated identity's
     * history.
     *
     * @param connectorId the connector whose create was refused
     */
    void recordScimUserCreateRejectedAsDuplicate(UUID connectorId);

    /**
     * Records a connector reading the User collection — a bulk read.
     *
     * <p>Takes no count and no filter, which is what keeps this boundary free of
     * text: the result count and the filter's shape arrive with the ticket that
     * implements filtering, as closed-set and numeric fields rather than as a
     * rendered query string.
     *
     * @param connectorId the connector that read the collection
     */
    void recordScimUsersListed(UUID connectorId);

    /**
     * Records an administrative change refused because of what it would leave behind.
     *
     * <p>A refusal about the ACTION rather than about the caller, who is an authenticated
     * administrator in every case — so it is not an authorization event and is not recorded
     * as one. It is recorded at all because each of these is an attempt to remove the
     * deployment's last way back in, and a run of them is a signal only the trail can carry:
     * the log line for a refused change deliberately names no identity.
     *
     * @param actorId   the administrator who asked, or {@code null} when their own identity
     *                  could not be resolved
     * @param subjectId the identity the change was aimed at
     */
    void recordAdministrativeChangeRefused(
            UUID actorId, UUID subjectId, AuditAdministrativeRefusal reason);

    /**
     * Records a connector creating a SCIM Group.
     *
     * @param groupId the created Group's stable id
     */
    void recordScimGroupCreated(UUID connectorId, UUID groupId);

    /**
     * Records a Group create refused.
     *
     * <p>No subject id, because there is none: the Group was not created, and the existing
     * resource that caused the refusal — a Group holding the name, a member id naming no
     * live User — is not what the event is about. Naming it would turn a refused create into
     * an event against an unrelated resource's history.
     */
    void recordScimGroupCreateRejected(UUID connectorId, AuditScimRefusal reason);

    /**
     * Records a connector changing a Group's name or membership. One operation for PUT and
     * PATCH, because the stored change is the same; which attributes moved is carried as the
     * event's changed paths.
     *
     * @param groupId the Group whose representation changed
     * @param changed which attributes moved — a closed set, because the recorded path list
     *                is a field readers filter on and a caller-assembled one is a place a
     *                submitted value could be written
     */
    void recordScimGroupReplaced(
            UUID connectorId, UUID groupId, Set<AuditGroupAttribute> changed);

    /**
     * Records a Group write refused — including an attempt to rename the Admin group or to
     * remove the Bootstrap Admin's membership of it, which carry
     * {@link AuditScimRefusal#MUTABILITY}.
     *
     * <p>This one DOES name the Group, unlike a refused create: the resource exists, the
     * write was aimed at it, and an attempt to provision the deployment's recovery authority
     * away is exactly the thing an administrator needs to find by that Group's id.
     */
    void recordScimGroupWriteRejected(
            UUID connectorId, UUID groupId, AuditScimRefusal reason);

    /** Records a connector deleting a Group. */
    void recordScimGroupDeleted(UUID connectorId, UUID groupId);

    /**
     * Records a connector reading the Group collection — a bulk read, whatever it asked for
     * and whatever came back, for the reason {@link #recordScimUsersListed} is recorded that
     * way.
     */
    void recordScimGroupsListed(UUID connectorId);

    /**
     * Records the server creating a resource it reserves for recovery, on a database that
     * did not have it.
     *
     * <p>No actor: seeding is the deployment establishing its own recovery path at startup,
     * not a principal acting, and inventing an actor for it would make the trail claim
     * somebody did this.
     *
     * <p>{@code group} rather than two methods, because the two seeded resources are one
     * act with one reason to exist — and a boolean that selects a closed-set resource type
     * keeps this boundary free of the {@code String} the type name would otherwise be.
     *
     * @param resourceId the seeded resource's stable id
     * @param group      whether the seeded resource is the Admin group rather than the
     *                   Bootstrap Admin User
     */
    void recordReservedResourceSeeded(UUID resourceId, boolean group);

    /**
     * Records that seeding put the Bootstrap Admin back into the Admin group, because something
     * outside SCIM had removed it.
     *
     * <p>A separate event from {@link #recordReservedResourceSeeded} because it is a different
     * fact: that one says a reserved resource was brought into existence, this one says a
     * deployment's administrative authority had been removed and startup restored it. Collapsing
     * them would make every restore read as a first-boot seed, which is the one reading that
     * would stop an operator investigating how the membership disappeared.
     *
     * <p>Audited at all because it is a WRITE that changes who holds Admin authority, and an
     * unaudited authority change is the gap this trail exists to close. It is also the only
     * authority change in the system with no actor — no principal asked for it, so inventing one
     * would make the trail claim somebody did this.
     *
     * @param groupId the Admin group's id
     * @param userId  the Bootstrap Admin whose membership was restored
     */
    void recordReservedMembershipRestored(UUID groupId, UUID userId);
}
