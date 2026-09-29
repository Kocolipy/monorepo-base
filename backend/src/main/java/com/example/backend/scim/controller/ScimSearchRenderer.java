package com.example.backend.scim.controller;

import com.example.backend.scim.application.ScimGroupResource;
import com.example.backend.scim.application.ScimListedResource;
import com.example.backend.scim.application.ScimSearchListing;
import com.example.backend.scim.application.ScimUserResource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A base search's page as SCIM's {@code ListResponse}: Users and Groups interleaved in the
 * query's order, each rendered exactly as its own endpoint renders it and projected with its own
 * type's projection.
 */
final class ScimSearchRenderer {

    private ScimSearchRenderer() {
    }

    static Map<String, Object> renderList(
            ScimSearchListing listing, String baseUri, ScimAttributeProjection.Search projection) {
        List<Map<String, Object>> resources = new ArrayList<>(listing.resources().size());
        for (ScimListedResource resource : listing.resources()) {
            resources.add(switch (resource) {
                case ScimUserResource user ->
                        projection.user().apply(ScimUserRenderer.render(user, baseUri));
                case ScimGroupResource group ->
                        projection.group().apply(ScimGroupRenderer.render(group, baseUri));
            });
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemas", List.of(ScimSchemas.LIST_RESPONSE));
        document.put("totalResults", listing.totalResults());
        document.put("startIndex", listing.page().startIndex());
        document.put("itemsPerPage", resources.size());
        if (!resources.isEmpty()) {
            document.put("Resources", List.copyOf(resources));
        }
        return document;
    }
}
