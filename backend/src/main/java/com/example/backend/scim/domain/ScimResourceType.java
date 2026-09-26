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
    USER("User"),

    /**
     * A collection of Users conferring authority. Declared now because the id
     * namespace it shares with {@link #USER} is the reason that namespace is a
     * table; the Group resource itself arrives with its own ticket.
     */
    GROUP("Group");

    private final String resourceTypeName;

    ScimResourceType(String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
    }

    /** The name RFC 7643 gives this resource type, exactly as it goes on the wire. */
    public String resourceTypeName() {
        return resourceTypeName;
    }
}
