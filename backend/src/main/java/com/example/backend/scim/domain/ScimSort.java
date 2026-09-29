package com.example.backend.scim.domain;

import com.example.backend.scim.domain.ScimFilter.AttributeRef;
import java.util.Locale;
import java.util.Set;

/**
 * RFC 7644 §3.4.2.3 sorting: one attribute and a direction.
 *
 * <p>The ordering rules are the translation's, stated here because they are this service's
 * contract: ascending is the default; a complex attribute is sorted through a sub-attribute and
 * never directly; a multi-valued attribute sorts by its primary value, or its first when none is
 * primary; a missing value sorts last ascending and first descending; and the resource id breaks
 * every remaining tie, so stateless paging returns each resource exactly once.
 *
 * @param attribute  what to sort by
 * @param descending whether the order is descending
 */
public record ScimSort(AttributeRef attribute, boolean descending) {

    /**
     * The sort the two parameters describe, or {@code null} when no {@code sortBy} was given.
     *
     * <p>A {@code sortOrder} without a {@code sortBy} is ignored rather than refused: it names a
     * direction for an order that is not being imposed, so there is nothing for it to be wrong
     * about.
     *
     * @throws InvalidScimQueryException when {@code sortBy} is not a sortable attribute of any
     *                                   queried type, or {@code sortOrder} is neither direction
     */
    public static ScimSort of(String sortBy, String sortOrder, Set<ScimResourceType> types) {
        boolean descending = descending(sortOrder);
        if (sortBy == null || sortBy.isBlank()) {
            return null;
        }
        ScimFilterParser.ResolvedPath path;
        try {
            path = ScimFilterParser.parsePath(sortBy, types);
        } catch (InvalidScimFilterException notSortable) {
            throw new InvalidScimQueryException(
                    "sortBy does not name a sortable attribute: " + notSortable.getMessage());
        }
        if (path.attribute().isComplex()) {
            throw new InvalidScimQueryException(
                    "sortBy on a complex attribute needs a sub-attribute: "
                            + path.attribute().path().canonical());
        }
        return new ScimSort(path.reference(), descending);
    }

    private static boolean descending(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return false;
        }
        return switch (sortOrder.trim().toLowerCase(Locale.ROOT)) {
            case "ascending" -> false;
            case "descending" -> true;
            default -> throw new InvalidScimQueryException(
                    "sortOrder must be ascending or descending.");
        };
    }
}
