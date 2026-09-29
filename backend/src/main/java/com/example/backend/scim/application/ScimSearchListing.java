package com.example.backend.scim.application;

import com.example.backend.scim.domain.ScimPageRequest;
import java.util.List;

/**
 * One page of a base search across Users and Groups, in the query's order.
 *
 * @param resources    the page's Users and Groups, interleaved as the query ordered them
 * @param totalResults how many resources of either type matched
 * @param page         the page that was asked for
 */
public record ScimSearchListing(
        List<ScimListedResource> resources, long totalResults, ScimPageRequest page) {

    public ScimSearchListing {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
