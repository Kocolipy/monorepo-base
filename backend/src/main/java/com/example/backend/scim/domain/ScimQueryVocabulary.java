package com.example.backend.scim.domain;

import static com.example.backend.scim.domain.ScimFilterPath.ACTIVE;
import static com.example.backend.scim.domain.ScimFilterPath.DISPLAY_NAME;
import static com.example.backend.scim.domain.ScimFilterPath.EMAILS;
import static com.example.backend.scim.domain.ScimFilterPath.EMAILS_PRIMARY;
import static com.example.backend.scim.domain.ScimFilterPath.EMAILS_TYPE;
import static com.example.backend.scim.domain.ScimFilterPath.EMAILS_VALUE;
import static com.example.backend.scim.domain.ScimFilterPath.EXTERNAL_ID;
import static com.example.backend.scim.domain.ScimFilterPath.GROUPS;
import static com.example.backend.scim.domain.ScimFilterPath.GROUPS_DISPLAY;
import static com.example.backend.scim.domain.ScimFilterPath.GROUPS_REF;
import static com.example.backend.scim.domain.ScimFilterPath.GROUPS_TYPE;
import static com.example.backend.scim.domain.ScimFilterPath.GROUPS_VALUE;
import static com.example.backend.scim.domain.ScimFilterPath.ID;
import static com.example.backend.scim.domain.ScimFilterPath.LOCALE;
import static com.example.backend.scim.domain.ScimFilterPath.MEMBERS;
import static com.example.backend.scim.domain.ScimFilterPath.MEMBERS_DISPLAY;
import static com.example.backend.scim.domain.ScimFilterPath.MEMBERS_REF;
import static com.example.backend.scim.domain.ScimFilterPath.MEMBERS_TYPE;
import static com.example.backend.scim.domain.ScimFilterPath.MEMBERS_VALUE;
import static com.example.backend.scim.domain.ScimFilterPath.META;
import static com.example.backend.scim.domain.ScimFilterPath.META_CREATED;
import static com.example.backend.scim.domain.ScimFilterPath.META_LAST_MODIFIED;
import static com.example.backend.scim.domain.ScimFilterPath.META_LOCATION;
import static com.example.backend.scim.domain.ScimFilterPath.META_RESOURCE_TYPE;
import static com.example.backend.scim.domain.ScimFilterPath.META_VERSION;
import static com.example.backend.scim.domain.ScimFilterPath.NAME;
import static com.example.backend.scim.domain.ScimFilterPath.NAME_FAMILY_NAME;
import static com.example.backend.scim.domain.ScimFilterPath.NAME_FORMATTED;
import static com.example.backend.scim.domain.ScimFilterPath.NAME_GIVEN_NAME;
import static com.example.backend.scim.domain.ScimFilterPath.NAME_HONORIFIC_PREFIX;
import static com.example.backend.scim.domain.ScimFilterPath.NAME_HONORIFIC_SUFFIX;
import static com.example.backend.scim.domain.ScimFilterPath.NAME_MIDDLE_NAME;
import static com.example.backend.scim.domain.ScimFilterPath.PREFERRED_LANGUAGE;
import static com.example.backend.scim.domain.ScimFilterPath.TIMEZONE;
import static com.example.backend.scim.domain.ScimFilterPath.USER_NAME;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which attribute paths one resource type can be queried by, and how each compares.
 *
 * <p>The type and case sensitivity here are the ones {@code /Schemas} advertises for the same
 * attribute, and a test holds the two equal: a connector reads {@code caseExact} from discovery
 * and builds its filters on it, so a filter that compared differently from what discovery says
 * would return a different set than the one the connector asked for. Every profile string is
 * case-insensitive, as RFC 7643 §2.2 and §8.7.1 make it; only identifiers are case-exact — the
 * common {@code id} and {@code externalId}, the {@code meta} values, and the {@code value} and
 * {@code $ref} of a membership, which are a resource id and its URI.
 *
 * <p>The common attributes ({@code id}, {@code externalId}, {@code meta}) are declared for both
 * types, because RFC 7643 §3.1 gives every resource them. {@code meta} is complex and its
 * sub-attributes are rendered values, so they are queryable as rendered: {@code meta.location}
 * and {@code meta.version} compare against the exact strings a client reads back.
 *
 * <p>{@code password} is absent from both, so no query can select, sort or probe by it.
 */
public final class ScimQueryVocabulary {

    /** How an attribute's values compare, as RFC 7643 §2.3 names the types this service has. */
    public enum Kind {
        STRING,
        BOOLEAN,
        DATE_TIME,
        REFERENCE,
        COMPLEX
    }

    /**
     * One queryable attribute of one resource type.
     *
     * @param path        the canonical path
     * @param kind        how its values compare
     * @param caseExact   whether string comparison respects case; meaningful for strings and
     *                    references only
     * @param multiValued whether the attribute, or the attribute this is a sub-attribute of, holds
     *                    many values
     */
    public record Attribute(ScimFilterPath path, Kind kind, boolean caseExact, boolean multiValued) {

        /** Whether this is a complex attribute, which a comparison must reach through a sub-path. */
        public boolean isComplex() {
            return kind == Kind.COMPLEX;
        }

        /** Whether this compares as text: a string or a reference. */
        public boolean isTextual() {
            return kind == Kind.STRING || kind == Kind.REFERENCE;
        }
    }

    private static final ScimQueryVocabulary USER = user();

    private static final ScimQueryVocabulary GROUP = group();

    private final Map<ScimFilterPath, Attribute> attributes;

    private ScimQueryVocabulary(Map<ScimFilterPath, Attribute> attributes) {
        this.attributes = Map.copyOf(attributes);
    }

    /** The vocabulary of one resource type. */
    public static ScimQueryVocabulary of(ScimResourceType type) {
        return type == ScimResourceType.USER ? USER : GROUP;
    }

    /** The attribute at this path, or empty when this resource type has no such attribute. */
    public Optional<Attribute> find(ScimFilterPath path) {
        return Optional.ofNullable(attributes.get(path));
    }

    /** Every queryable path of this resource type. */
    public Set<ScimFilterPath> paths() {
        return attributes.keySet();
    }

    private static ScimQueryVocabulary user() {
        Map<ScimFilterPath, Attribute> user = common();
        text(user, USER_NAME, false);
        user.put(NAME, new Attribute(NAME, Kind.COMPLEX, false, false));
        for (ScimFilterPath sub : new ScimFilterPath[] {
                NAME_FORMATTED, NAME_FAMILY_NAME, NAME_GIVEN_NAME, NAME_MIDDLE_NAME,
                NAME_HONORIFIC_PREFIX, NAME_HONORIFIC_SUFFIX}) {
            text(user, sub, false);
        }
        text(user, DISPLAY_NAME, false);
        text(user, PREFERRED_LANGUAGE, false);
        text(user, LOCALE, false);
        text(user, TIMEZONE, false);
        user.put(ACTIVE, new Attribute(ACTIVE, Kind.BOOLEAN, false, false));
        multi(user, EMAILS, Kind.COMPLEX, false);
        multi(user, EMAILS_VALUE, Kind.STRING, false);
        multi(user, EMAILS_TYPE, Kind.STRING, false);
        multi(user, EMAILS_PRIMARY, Kind.BOOLEAN, false);
        multi(user, GROUPS, Kind.COMPLEX, false);
        multi(user, GROUPS_VALUE, Kind.STRING, true);
        multi(user, GROUPS_DISPLAY, Kind.STRING, false);
        multi(user, GROUPS_REF, Kind.REFERENCE, true);
        multi(user, GROUPS_TYPE, Kind.STRING, false);
        return new ScimQueryVocabulary(user);
    }

    private static ScimQueryVocabulary group() {
        Map<ScimFilterPath, Attribute> group = common();
        text(group, DISPLAY_NAME, false);
        multi(group, MEMBERS, Kind.COMPLEX, false);
        multi(group, MEMBERS_VALUE, Kind.STRING, true);
        multi(group, MEMBERS_DISPLAY, Kind.STRING, false);
        multi(group, MEMBERS_REF, Kind.REFERENCE, true);
        multi(group, MEMBERS_TYPE, Kind.STRING, false);
        return new ScimQueryVocabulary(group);
    }

    /** The RFC 7643 §3.1 attributes every resource has. {@code id} and {@code externalId} are case-exact. */
    private static Map<ScimFilterPath, Attribute> common() {
        Map<ScimFilterPath, Attribute> common = new EnumMap<>(ScimFilterPath.class);
        text(common, ID, true);
        text(common, EXTERNAL_ID, true);
        common.put(META, new Attribute(META, Kind.COMPLEX, true, false));
        text(common, META_RESOURCE_TYPE, true);
        common.put(META_CREATED, new Attribute(META_CREATED, Kind.DATE_TIME, true, false));
        common.put(
                META_LAST_MODIFIED, new Attribute(META_LAST_MODIFIED, Kind.DATE_TIME, true, false));
        common.put(META_LOCATION, new Attribute(META_LOCATION, Kind.REFERENCE, true, false));
        text(common, META_VERSION, true);
        return common;
    }

    private static void text(Map<ScimFilterPath, Attribute> into, ScimFilterPath path, boolean exact) {
        into.put(path, new Attribute(path, Kind.STRING, exact, false));
    }

    /** A multi-valued attribute or one of its sub-attributes. */
    private static void multi(
            Map<ScimFilterPath, Attribute> into, ScimFilterPath path, Kind kind, boolean exact) {
        into.put(path, new Attribute(path, kind, exact, true));
    }
}
