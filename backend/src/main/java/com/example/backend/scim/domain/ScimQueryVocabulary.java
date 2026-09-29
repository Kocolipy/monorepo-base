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
 * would return a different set than the one the connector asked for. {@code displayName} is the
 * case that makes the rule matter — case-exact on a User, case-insensitive on a Group, because
 * the Group's is server-unique on its normalized form.
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
        user.put(NAME, new Attribute(NAME, Kind.COMPLEX, true, false));
        for (ScimFilterPath sub : new ScimFilterPath[] {
                NAME_FORMATTED, NAME_FAMILY_NAME, NAME_GIVEN_NAME, NAME_MIDDLE_NAME,
                NAME_HONORIFIC_PREFIX, NAME_HONORIFIC_SUFFIX}) {
            text(user, sub, true);
        }
        text(user, DISPLAY_NAME, true);
        text(user, PREFERRED_LANGUAGE, true);
        text(user, LOCALE, true);
        text(user, TIMEZONE, true);
        user.put(ACTIVE, new Attribute(ACTIVE, Kind.BOOLEAN, true, false));
        multi(user, EMAILS, Kind.COMPLEX);
        multi(user, EMAILS_VALUE, Kind.STRING);
        multi(user, EMAILS_TYPE, Kind.STRING);
        multi(user, EMAILS_PRIMARY, Kind.BOOLEAN);
        multi(user, GROUPS, Kind.COMPLEX);
        multi(user, GROUPS_VALUE, Kind.STRING);
        multi(user, GROUPS_DISPLAY, Kind.STRING);
        multi(user, GROUPS_REF, Kind.REFERENCE);
        multi(user, GROUPS_TYPE, Kind.STRING);
        return new ScimQueryVocabulary(user);
    }

    private static ScimQueryVocabulary group() {
        Map<ScimFilterPath, Attribute> group = common();
        text(group, DISPLAY_NAME, false);
        multi(group, MEMBERS, Kind.COMPLEX);
        multi(group, MEMBERS_VALUE, Kind.STRING);
        multi(group, MEMBERS_DISPLAY, Kind.STRING);
        multi(group, MEMBERS_REF, Kind.REFERENCE);
        multi(group, MEMBERS_TYPE, Kind.STRING);
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

    /** A multi-valued attribute or one of its sub-attributes; every one of them is case-exact. */
    private static void multi(Map<ScimFilterPath, Attribute> into, ScimFilterPath path, Kind kind) {
        into.put(path, new Attribute(path, kind, true, true));
    }
}
