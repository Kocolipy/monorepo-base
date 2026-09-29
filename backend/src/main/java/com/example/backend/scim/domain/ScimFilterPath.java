package com.example.backend.scim.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Every attribute path a SCIM query can name, in its canonical spelling.
 *
 * <p>The query protocol's vocabulary, as one closed set. A filter, a {@code sortBy} and the
 * audit record of a filter's shape all speak in these constants rather than in the strings a
 * client sent, which is what makes three things true at once: an attribute name a client
 * spelled {@code USERNAME} is the same attribute as {@code userName}; nothing a client typed
 * reaches a SQL statement or an audit row as text; and the translation to storage is an
 * exhaustive switch the compiler checks, rather than a lookup that can miss.
 *
 * <p>The set is the union of both resource types' attributes. Which of them a given resource
 * type actually has — and with what type and case sensitivity — is {@link ScimQueryVocabulary}'s
 * business, because {@code displayName} is a path of both and means a different comparison on
 * each.
 *
 * <p>{@link #PASSWORD} is here although no resource type lets it be queried. It is recognized
 * so that naming it is refused as what it is — a credential, never filterable — rather than
 * as an attribute this service has never heard of, which would be a false statement about the
 * schema.
 */
public enum ScimFilterPath {

    ID("id", null),
    EXTERNAL_ID("externalId", null),
    USER_NAME("userName", null),
    NAME("name", null),
    NAME_FORMATTED("formatted", NAME),
    NAME_FAMILY_NAME("familyName", NAME),
    NAME_GIVEN_NAME("givenName", NAME),
    NAME_MIDDLE_NAME("middleName", NAME),
    NAME_HONORIFIC_PREFIX("honorificPrefix", NAME),
    NAME_HONORIFIC_SUFFIX("honorificSuffix", NAME),
    DISPLAY_NAME("displayName", null),
    PREFERRED_LANGUAGE("preferredLanguage", null),
    LOCALE("locale", null),
    TIMEZONE("timezone", null),
    ACTIVE("active", null),
    PASSWORD("password", null),
    EMAILS("emails", null),
    EMAILS_VALUE("value", EMAILS),
    EMAILS_TYPE("type", EMAILS),
    EMAILS_PRIMARY("primary", EMAILS),
    GROUPS("groups", null),
    GROUPS_VALUE("value", GROUPS),
    GROUPS_DISPLAY("display", GROUPS),
    GROUPS_REF("$ref", GROUPS),
    GROUPS_TYPE("type", GROUPS),
    MEMBERS("members", null),
    MEMBERS_VALUE("value", MEMBERS),
    MEMBERS_DISPLAY("display", MEMBERS),
    MEMBERS_REF("$ref", MEMBERS),
    MEMBERS_TYPE("type", MEMBERS),
    META("meta", null),
    META_RESOURCE_TYPE("resourceType", META),
    META_CREATED("created", META),
    META_LAST_MODIFIED("lastModified", META),
    META_LOCATION("location", META),
    META_VERSION("version", META);

    private final String name;

    private final ScimFilterPath parent;

    ScimFilterPath(String name, ScimFilterPath parent) {
        this.name = name;
        this.parent = parent;
    }

    /**
     * The path an attribute name and an optional sub-attribute name denote, matched
     * case-insensitively as RFC 7644 §3.10 requires of attribute names.
     *
     * @param attribute    the top-level attribute name
     * @param subAttribute the sub-attribute name, or {@code null} for the attribute itself
     */
    public static Optional<ScimFilterPath> of(String attribute, String subAttribute) {
        Optional<ScimFilterPath> top = Arrays.stream(values())
                .filter(path -> path.parent == null && path.name.equalsIgnoreCase(attribute))
                .findFirst();
        if (subAttribute == null || top.isEmpty()) {
            return top;
        }
        return top.get().subAttribute(subAttribute);
    }

    /** The sub-attribute of this path with that name, matched case-insensitively. */
    public Optional<ScimFilterPath> subAttribute(String subAttribute) {
        return Arrays.stream(values())
                .filter(path -> path.parent == this && path.name.equalsIgnoreCase(subAttribute))
                .findFirst();
    }

    /** This path's sub-attributes, in declaration order; empty for a simple attribute. */
    public List<ScimFilterPath> subAttributes() {
        return Arrays.stream(values()).filter(path -> path.parent == this).toList();
    }

    /** The attribute this is a sub-attribute of, or {@code null} for a top-level attribute. */
    public ScimFilterPath parent() {
        return parent;
    }

    /** The name of this attribute alone — {@code familyName} for {@code name.familyName}. */
    public String attributeName() {
        return name;
    }

    /** The canonical dotted path: {@code userName}, {@code name.familyName}. */
    public String canonical() {
        return parent == null ? name : parent.name + "." + name;
    }
}
