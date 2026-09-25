package com.example.backend.scim.domain;

import java.util.UUID;

/**
 * Who a SCIM request is, once its bearer token has been verified.
 *
 * <p>Ids and a closed set, and no display name: what the rest of the request needs
 * to know is which connector is acting, which token it acted with — so a refusal
 * or an audit event names the credential and not only its owner — and what it is
 * allowed to do. A connector's display name is an Admin's label and has no place in
 * an authorisation decision.
 *
 * @param connectorId the acting connector's stable id
 * @param tokenId     the token it presented
 * @param scope       what that token authorises, directory-wide
 */
public record AuthenticatedConnector(UUID connectorId, UUID tokenId, ConnectorTokenScope scope) {
}
