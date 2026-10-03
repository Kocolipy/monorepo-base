package com.example.backend.scim.controller;

import com.example.backend.scim.domain.ScimAttribute;
import com.example.backend.scim.domain.ScimResourceSchema;
import com.example.backend.scim.domain.ScimResourceType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code /Schemas} documents, rendered from {@link ScimResourceSchema}.
 *
 * <p>Rendering only. Which attributes exist and what is true of each is the domain schema's
 * business; this class owns how RFC 7643 §7 spells those facts on the wire — {@code readWrite},
 * {@code dateTime}, which keys are omitted when empty — so a change of spelling never touches the
 * definition, and a change of definition never needs a second edit here.
 *
 * <p>A schema document lists the attributes its schema ADDS, so the RFC 7643 §3.1 common
 * attributes ({@link ScimResourceSchema#COMMON}) are never rendered as User or Group attributes.
 */
final class ScimSchemaDocuments {

    private ScimSchemaDocuments() {
    }

    /** The core schema of one resource type, as {@code /Schemas} renders it under {@code baseUri}. */
    static Map<String, Object> of(ScimResourceType type, String baseUri) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemas", List.of(ScimSchemas.SCHEMA));
        document.put("id", type.schemaUri());
        document.put("name", type.resourceTypeName());
        document.put("description",
                "SCIM core " + type.resourceTypeName() + ", as implemented by this service.");
        document.put("attributes", ScimResourceSchema.of(type).attributes().stream()
                .map(ScimSchemaDocuments::render)
                .toList());
        document.put("meta", Map.of(
                "resourceType", "Schema",
                "location", baseUri + "/Schemas/" + type.schemaUri()));
        return document;
    }

    /** One attribute as a schema document renders it. */
    static Map<String, Object> render(ScimAttribute attribute) {
        Map<String, Object> rendered = new LinkedHashMap<>();
        rendered.put("name", attribute.name());
        rendered.put("type", type(attribute));
        if (!attribute.subAttributes().isEmpty()) {
            rendered.put("subAttributes", attribute.subAttributes().stream()
                    .map(ScimSchemaDocuments::render)
                    .toList());
        }
        rendered.put("multiValued", attribute.multiValued());
        rendered.put("required", attribute.required());
        rendered.put("caseExact", attribute.caseExact());
        rendered.put("mutability", mutability(attribute));
        rendered.put("returned", returned(attribute));
        rendered.put("uniqueness", uniqueness(attribute));
        if (!attribute.canonicalValues().isEmpty()) {
            rendered.put("canonicalValues", attribute.canonicalValues());
        }
        if (!attribute.referenceTypes().isEmpty()) {
            rendered.put("referenceTypes", attribute.referenceTypes());
        }
        return rendered;
    }

    private static String type(ScimAttribute attribute) {
        return switch (attribute.type()) {
            case STRING -> "string";
            case BOOLEAN -> "boolean";
            case DATE_TIME -> "dateTime";
            case REFERENCE -> "reference";
            case COMPLEX -> "complex";
        };
    }

    private static String mutability(ScimAttribute attribute) {
        return switch (attribute.mutability()) {
            case READ_ONLY -> "readOnly";
            case READ_WRITE -> "readWrite";
            case WRITE_ONLY -> "writeOnly";
        };
    }

    private static String returned(ScimAttribute attribute) {
        return switch (attribute.returned()) {
            case ALWAYS -> "always";
            case DEFAULT -> "default";
            case NEVER -> "never";
        };
    }

    private static String uniqueness(ScimAttribute attribute) {
        return switch (attribute.uniqueness()) {
            case NONE -> "none";
            case SERVER -> "server";
        };
    }
}
