package com.example.backend.scim.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What an Admin may know about a connector: its identity, and the state of every
 * token it holds.
 *
 * <p>Tokens are included rather than fetched separately because the question an
 * Admin has about a connector is almost always about its credentials — whether one
 * is expiring, whether a rotation left two live. A separate endpoint would make the
 * common view two round trips and let the two answers disagree.
 *
 * @param id          stable id, which every token and audit event names
 * @param displayName the Admin's own label for this integration
 * @param createdAt   when it was created
 * @param tokens      every token it has held, newest issue first
 */
public record ConnectorSummary(
        UUID id, String displayName, Instant createdAt, List<ConnectorTokenSummary> tokens) {

    public ConnectorSummary {
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
    }
}
