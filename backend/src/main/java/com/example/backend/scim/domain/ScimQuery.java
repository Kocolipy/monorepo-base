package com.example.backend.scim.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One collection query — a filter, a sort and a page — over one or both resource types.
 *
 * <p>The same value whether it arrived as query parameters on a {@code GET} or as the body of a
 * {@code POST .search}, which is what makes the two paths return the same resources: there is
 * one query, and two ways of writing it down.
 *
 * @param types  the resource types being queried; both for a base search
 * @param filter the filter, or {@code null} for every resource
 * @param sort   the requested order, or {@code null} for this service's default order
 * @param page   which slice of the ordered result to return
 */
public record ScimQuery(
        Set<ScimResourceType> types, ScimFilter filter, ScimSort sort, ScimPageRequest page) {

    public ScimQuery {
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("a query names at least one resource type");
        }
        types = Set.copyOf(types);
        page = page == null ? ScimPageRequest.FIRST_PAGE : page;
    }

    /**
     * The query the textual parameters describe.
     *
     * @throws InvalidScimFilterException when the filter is not one this service evaluates
     * @throws InvalidScimQueryException  when the sort is not
     */
    public static ScimQuery of(
            Set<ScimResourceType> types,
            String filter,
            String sortBy,
            String sortOrder,
            ScimPageRequest page) {
        ScimFilter parsed = filter == null ? null : ScimFilterParser.parse(filter, types);
        return new ScimQuery(types, parsed, ScimSort.of(sortBy, sortOrder, types), page);
    }

    /**
     * What one query found: the total that matched, and the identities of the page, in order.
     *
     * @param totalResults how many resources matched, irrespective of the page
     * @param hits         the page's resources, in the query's order
     */
    public record Result(long totalResults, List<Hit> hits) {

        public Result {
            hits = List.copyOf(hits);
        }

        /** The ids of the hits of one resource type, in order. */
        public List<UUID> idsOf(ScimResourceType type) {
            return hits.stream().filter(hit -> hit.type() == type).map(Hit::id).toList();
        }
    }

    /** One matched resource, identified. */
    public record Hit(ScimResourceType type, UUID id) {
    }
}
