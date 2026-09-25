package com.example.backend.scim.application;

import com.example.backend.audit.domain.AuditTrail;
import com.example.backend.auth.domain.Account;
import com.example.backend.auth.domain.AccountRepository;
import com.example.backend.observability.LogEvent;
import com.example.backend.scim.domain.ConnectorTokenPolicy;
import com.example.backend.scim.domain.ConnectorTokenScope;
import com.example.backend.scim.domain.ConnectorTokenSecret;
import com.example.backend.scim.domain.ScimConnector;
import com.example.backend.scim.domain.ScimConnectorRepository;
import com.example.backend.scim.domain.ScimConnectorToken;
import com.example.backend.scim.domain.ScimConnectorTokenRepository;
import com.example.backend.scim.domain.ScimExternalIdRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The connector and token use cases an administrator drives.
 *
 * <p>Separate from {@link ConnectorAuthenticationService}, which serves the request
 * path, for the reason {@code AccountAdministrationService} is separate from
 * {@code AccountService}: the class that verifies a presented credential does not
 * also mint and revoke them, so nothing on the authentication path can reach a
 * write by accident.
 *
 * <p>A minted plaintext value leaves this class exactly once, in the
 * {@link IssuedConnectorToken} the issue and rotation methods return. It is never
 * held in a field, never logged, and never written anywhere but that response — what
 * is persisted is its digest.
 */
@Service
public class ConnectorAdministrationService {

    private static final Logger log =
            LoggerFactory.getLogger(ConnectorAdministrationService.class);

    private static final String CREATE_ACTION = "scim.connector.create";
    private static final String DELETE_ACTION = "scim.connector.delete";
    private static final String ISSUE_ACTION = "scim.connector.token.issue";
    private static final String ROTATE_ACTION = "scim.connector.token.rotate";
    private static final String REVOKE_ACTION = "scim.connector.token.revoke";

    private final ScimConnectorRepository connectors;
    private final ScimConnectorTokenRepository tokens;
    private final ScimExternalIdRepository aliases;
    private final AccountRepository accounts;
    private final AuditTrail audit;
    private final SecureRandom random;
    private final Clock clock;

    public ConnectorAdministrationService(
            ScimConnectorRepository connectors,
            ScimConnectorTokenRepository tokens,
            ScimExternalIdRepository aliases,
            AccountRepository accounts,
            AuditTrail audit,
            SecureRandom random,
            Clock clock) {
        this.connectors = connectors;
        this.tokens = tokens;
        this.aliases = aliases;
        this.accounts = accounts;
        this.audit = audit;
        this.random = random;
        this.clock = clock;
    }

    /** Every live connector with the state of its tokens. Never carries a digest. */
    public List<ConnectorSummary> listConnectors() {
        Instant now = clock.instant();
        return connectors.findAllLiveOrderedByDisplayName().stream()
                .map(connector -> summarize(connector, now))
                .toList();
    }

    /** Creates a connector holding no tokens yet. */
    @Transactional
    public ConnectorSummary create(String displayName, String requestedBy) {
        ScimConnector connector =
                ScimConnector.create(UUID.randomUUID(), displayName, clock.instant());
        connectors.save(connector);
        audit.recordConnectorCreated(actorId(requestedBy), connector.id());
        succeeded(CREATE_ACTION);
        return summarize(connector, clock.instant());
    }

    /**
     * Deletes a connector: every token it holds is revoked and every
     * {@code externalId} alias it owns is removed, in this one transaction.
     *
     * <p>One transaction is the requirement rather than an optimisation. A partial
     * delete is the one outcome that must be impossible: aliases gone with tokens
     * still live would leave a credential able to write to a directory it can no
     * longer address its own resources in, and tokens gone with aliases surviving
     * would leave a namespace reserved by nobody, blocking the reuse the RFC allows.
     * Both writes and the connector's own transition therefore commit or roll back
     * together, and the audit event joins them — it is fail-closed, so a delete this
     * service cannot account for does not happen.
     *
     * <p>Users and Groups are untouched. An alias is a connector's name for a
     * resource, not the resource.
     *
     * <p>Idempotent in effect: deleting an already-deleted connector is refused with
     * {@link UnknownConnectorException}, because an Admin repeating a delete is more
     * likely to have the wrong id than to want a second no-op.
     */
    @Transactional
    public void delete(UUID connectorId, String requestedBy) {
        ScimConnector connector = requireLive(connectorId);
        Instant now = clock.instant();
        int revoked = tokens.revokeAllForConnector(connectorId, now);
        aliases.deleteAllForConnector(connectorId);
        connectors.save(connector.deleted(now));
        // The connector's own event carries the actor; the per-token events do not,
        // so the trail reads as one administrative act with the credentials it took
        // down rather than as several independent revocations.
        for (int token = 0; token < revoked; token++) {
            audit.recordConnectorTokenRevoked(null, connectorId);
        }
        audit.recordConnectorDeleted(actorId(requestedBy), connectorId);
        succeeded(DELETE_ACTION);
    }

    /**
     * Mints a token for a connector. The returned plaintext is the only copy that
     * will ever exist.
     *
     * @param lifetime how long it should live, or {@code null} for the default;
     *                 bounded by {@link ConnectorTokenPolicy}
     */
    @Transactional
    public IssuedConnectorToken issueToken(
            UUID connectorId, ConnectorTokenScope scope, Duration lifetime, String requestedBy) {
        requireLive(connectorId);
        Instant now = clock.instant();
        ConnectorTokenSecret.Minted minted = ConnectorTokenSecret.mint(random);
        ScimConnectorToken issued = ScimConnectorToken.issue(
                UUID.randomUUID(),
                connectorId,
                minted.lookupId(),
                minted.digest(),
                scope,
                now,
                now.plus(ConnectorTokenPolicy.lifetime(lifetime)));
        tokens.save(issued);
        audit.recordConnectorTokenIssued(actorId(requestedBy), connectorId);
        succeeded(ISSUE_ACTION);
        return disclose(issued, minted.presentedValue());
    }

    /**
     * Replaces a token with a new one of the same scope, leaving the old one usable
     * until the overlap window ends.
     *
     * <p>The old token's expiry is brought forward to the end of the overlap and
     * never pushed back: the window ends at the earlier of the requested overlap and
     * the expiry the token already had, so a rotation cannot buy a token more life
     * than it was issued with — nor more than a previous rotation left it. An overlap
     * of zero, or none at all, ends the old token at this instant.
     *
     * <p>The new token's own lifetime is a fresh full lifetime, not the remainder of
     * the old one's: rotation exists so an integration can keep running, and handing
     * it a replacement that expires next week would defeat that.
     *
     * @param overlap how long the old token should keep working, clamped to
     *                {@link ConnectorTokenPolicy#MAX_ROTATION_OVERLAP}
     */
    @Transactional
    public IssuedConnectorToken rotateToken(UUID tokenId, Duration overlap, String requestedBy) {
        ScimConnectorToken existing = requireToken(tokenId);
        ScimConnector connector = requireLive(existing.connectorId());
        Instant now = clock.instant();

        ConnectorTokenSecret.Minted minted = ConnectorTokenSecret.mint(random);
        ScimConnectorToken replacement = ScimConnectorToken.issue(
                UUID.randomUUID(),
                connector.id(),
                minted.lookupId(),
                minted.digest(),
                existing.scope(),
                now,
                now.plus(ConnectorTokenPolicy.DEFAULT_LIFETIME));
        tokens.save(replacement);
        tokens.save(existing.expiringAt(
                ConnectorTokenPolicy.overlapEnd(now, overlap, existing.expiresAt()),
                replacement.id()));

        audit.recordConnectorTokenRotated(actorId(requestedBy), connector.id());
        succeeded(ROTATE_ACTION);
        return disclose(replacement, minted.presentedValue());
    }

    /**
     * Revokes a token, effective immediately.
     *
     * <p>Idempotent: a token already revoked keeps its original revocation time and
     * nothing is written, but the event is still recorded — an administrator
     * revoking a credential twice is a thing worth being able to see.
     */
    @Transactional
    public void revokeToken(UUID tokenId, String requestedBy) {
        ScimConnectorToken existing = requireToken(tokenId);
        ScimConnectorToken revoked = existing.revoked(clock.instant());
        if (revoked != existing) {
            tokens.save(revoked);
        }
        audit.recordConnectorTokenRevoked(actorId(requestedBy), existing.connectorId());
        succeeded(REVOKE_ACTION);
    }

    /**
     * The stable id behind the administrator's username, for the event's actor
     * reference. {@code null} when the name resolves to no account — the same call
     * {@code AccountAdministrationService} makes, and for the same reason: an event
     * with no actor is more use than no event, and what it never becomes is the
     * username itself.
     */
    private UUID actorId(String requestedBy) {
        return accounts.findByUsername(requestedBy).map(Account::id).orElse(null);
    }

    private ScimConnector requireLive(UUID connectorId) {
        return connectors.findById(connectorId)
                .filter(connector -> !connector.isDeleted())
                .orElseThrow(() -> new UnknownConnectorException(
                        "No live connector with that id"));
    }

    private ScimConnectorToken requireToken(UUID tokenId) {
        return tokens.findById(tokenId)
                .orElseThrow(() -> new UnknownConnectorException("No token with that id"));
    }

    private ConnectorSummary summarize(ScimConnector connector, Instant now) {
        return new ConnectorSummary(
                connector.id(),
                connector.displayName(),
                connector.createdAt(),
                tokens.findByConnectorIdOrderedByIssuedAtDesc(connector.id()).stream()
                        .map(token -> summarize(token, now))
                        .toList());
    }

    private static ConnectorTokenSummary summarize(ScimConnectorToken token, Instant now) {
        return new ConnectorTokenSummary(
                token.id(),
                token.scope(),
                token.issuedAt(),
                token.expiresAt(),
                token.originalExpiresAt(),
                token.revokedAt(),
                token.isUsable(now));
    }

    private static IssuedConnectorToken disclose(ScimConnectorToken token, String presentedValue) {
        return new IssuedConnectorToken(
                token.connectorId(),
                token.id(),
                token.scope(),
                token.issuedAt(),
                token.expiresAt(),
                presentedValue);
    }

    /**
     * Records that a connector or token lifecycle write went through.
     *
     * <p>The action and the outcome, and nothing else. No connector id, no token id,
     * and above all no token value: the log stream is read by more people and
     * retained longer than the database, so naming who changed which credential is
     * the audit trail's job — which does it by stable id, and is the thing an
     * administrator investigating a connector actually reads.
     */
    private static void succeeded(String action) {
        log.atInfo()
                .addKeyValue(LogEvent.ACTION, action)
                .addKeyValue(LogEvent.OUTCOME, LogEvent.SUCCESS)
                .log("SCIM connector lifecycle change applied");
    }
}
