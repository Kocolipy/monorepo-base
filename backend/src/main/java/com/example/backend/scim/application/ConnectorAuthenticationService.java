package com.example.backend.scim.application;

import com.example.backend.scim.domain.AuthenticatedConnector;
import com.example.backend.scim.domain.ConnectorTokenSecret;
import com.example.backend.scim.domain.ScimConnectorRepository;
import com.example.backend.scim.domain.ScimConnectorToken;
import com.example.backend.scim.domain.ScimConnectorTokenRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a presented bearer value into the connector it authenticates, or into
 * nothing.
 *
 * <p>Read-only and mints nothing, which is why it is separate from
 * {@link ConnectorAdministrationService}: the class on the request path holds no
 * capability to create or revoke a credential, so no SCIM request can reach one.
 *
 * <p><strong>One refusal, four reasons.</strong> A malformed value, an unknown
 * lookup id, a wrong secret, an expired or revoked token and a deleted connector all
 * return the same empty result, and the filter turns all of them into the same
 * {@code invalid_token}. Distinguishing them would tell a caller holding a stale
 * credential whether it ever existed, whether it was revoked deliberately, and
 * whether the connector is still configured — three facts that help whoever is
 * probing more than whoever owns the integration.
 */
@Service
public class ConnectorAuthenticationService {

    private final ScimConnectorRepository connectors;
    private final ScimConnectorTokenRepository tokens;
    private final Clock clock;

    public ConnectorAuthenticationService(
            ScimConnectorRepository connectors,
            ScimConnectorTokenRepository tokens,
            Clock clock) {
        this.connectors = connectors;
        this.tokens = tokens;
        this.clock = clock;
    }

    /**
     * The connector this value authenticates, or empty.
     *
     * <p>The digest comparison is constant time ({@link
     * com.example.backend.scim.domain.ConnectorTokenDigest#matches}), and the value
     * is parsed before anything reaches the database, so a caller's arbitrary string
     * is never used as a query key.
     *
     * @param presentedValue the complete opaque value from the {@code Authorization}
     *                       header — the only place this service reads one from
     */
    @Transactional(readOnly = true)
    public Optional<AuthenticatedConnector> authenticate(String presentedValue) {
        return ConnectorTokenSecret.parse(presentedValue)
                .flatMap(presented -> tokens.findByLookupId(presented.lookupId())
                        .filter(token -> token.digest().matches(presented.digest())))
                .filter(token -> token.isUsable(clock.instant()))
                .filter(this::connectorIsLive)
                .map(ConnectorAuthenticationService::asPrincipal);
    }

    /**
     * Whether the token's connector still exists. Checked separately from the
     * token's own state because deleting a connector revokes its tokens in the same
     * transaction — so this can only be false if that transaction was somehow
     * incomplete, and a credential whose owner is gone is refused rather than
     * trusted on the strength of the revocation having worked.
     */
    private boolean connectorIsLive(ScimConnectorToken token) {
        return connectors.findById(token.connectorId())
                .filter(connector -> !connector.isDeleted())
                .isPresent();
    }

    private static AuthenticatedConnector asPrincipal(ScimConnectorToken token) {
        return new AuthenticatedConnector(token.connectorId(), token.id(), token.scope());
    }
}
