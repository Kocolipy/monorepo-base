package com.example.backend.scim.domain;

/**
 * The kinds of resource that share the SCIM id namespace.
 *
 * <p>A closed set, and the spelling matters: {@link #resourceTypeName()} is what
 * {@code meta.resourceType} renders and what the {@code scim_resources} table
 * stores, so the enum constant and the wire value are related by one method here
 * rather than by a mapping at each use.
 */
public enum ScimResourceType {

    /** A person or provisioning identity. */
    USER("User", "urn:ietf:params:scim:schemas:core:2.0:User"),

    /** A collection of Users conferring authority. */
    GROUP("Group", "urn:ietf:params:scim:schemas:core:2.0:Group");

    private final String resourceTypeName;

    private final String schemaUri;

    ScimResourceType(String resourceTypeName, String schemaUri) {
        this.resourceTypeName = resourceTypeName;
        this.schemaUri = schemaUri;
    }

    /** The name RFC 7643 gives this resource type, exactly as it goes on the wire. */
    public String resourceTypeName() {
        return resourceTypeName;
    }

    /**
     * The URI of this type's core schema, which a filter path may be qualified with —
     * {@code urn:ietf:params:scim:schemas:core:2.0:User:userName}. The same string the web
     * adapter renders in {@code schemas}; a test holds the two equal.
     */
    public String schemaUri() {
        return schemaUri;
    }
}
