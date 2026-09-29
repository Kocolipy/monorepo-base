package com.example.backend.scim.domain;

import java.util.UUID;

/**
 * Persistence port for SCIM collection queries: which resources a filter selects, in what
 * order, and how many there are.
 *
 * <p>Answers with identities rather than resources, so the loading of a page stays with the
 * repositories that already know how to assemble a User or a Group, and this port's one job is
 * the part only a database can do efficiently — evaluating the filter and the order over every
 * resource.
 *
 * <p>An implementation must bind every literal the filter carries as a statement parameter; no
 * value from a {@link ScimFilter} may become statement text.
 */
public interface ScimQueryRepository {

    /**
     * The query's total and its page.
     *
     * <p>A page of zero resources is still a query: {@code count=0} is how a client asks how
     * many there are, so the total is computed and no page is read.
     *
     * @param query       the query
     * @param connectorId the calling connector, whose aliases are the {@code externalId} values
     *                    the query sees
     * @param baseUri     the absolute SCIM base URI, from which {@code $ref} and
     *                    {@code meta.location} are computed exactly as they are rendered
     */
    ScimQuery.Result query(ScimQuery query, UUID connectorId, String baseUri);
}
