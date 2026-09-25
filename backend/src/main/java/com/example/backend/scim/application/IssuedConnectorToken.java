package com.example.backend.scim.application;

import com.example.backend.scim.domain.ConnectorTokenScope;
import java.time.Instant;
import java.util.UUID;

/**
 * A token just minted, and the one and only time its plaintext exists outside the
 * connector that will use it.
 *
 * <p>Returned by issue and rotation, and by nothing else. There is no way back to
 * this value: what was stored is a digest, so a second request for it would have to
 * mint a different token. A type of its own rather than a nullable field on
 * {@link ConnectorTokenSummary} for exactly that reason — a listing that returned
 * {@code null} plaintext would be one bug away from returning a real one, and the
 * type an Admin lists tokens through should have no field for it at all.
 *
 * <p>The responses carrying this are marked {@code Cache-Control: no-store}, so the
 * value is not written to a shared cache on its way to the Admin's browser.
 *
 * @param connectorId    the connector this token authenticates
 * @param tokenId        the token's stable id, for a later rotate or revoke
 * @param scope          what it authorises
 * @param issuedAt       when it was minted
 * @param expiresAt      when it stops being accepted
 * @param presentedValue the complete opaque bearer value, to be sent as
 *                       {@code Authorization: Bearer <value>} and stored by the
 *                       Admin, because this service cannot show it again
 */
public record IssuedConnectorToken(
        UUID connectorId,
        UUID tokenId,
        ConnectorTokenScope scope,
        Instant issuedAt,
        Instant expiresAt,
        String presentedValue) {
}
