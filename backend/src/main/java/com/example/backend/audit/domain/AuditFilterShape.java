package com.example.backend.audit.domain;

import java.util.Locale;

/**
 * The shape of a SCIM filter, as a bulk-read audit event records it: which attributes were
 * compared with which operators under which logical structure — and never the values.
 *
 * <p>A tree of closed sets rather than a string, and that is the point of it. A filter is the
 * preferred carrier of personal values ({@code userName eq "a.person@example.com"}), and the
 * shape is what lets an investigator tell a full-directory sync from a targeted probe without
 * the trail itself becoming a copy of what was probed for. A {@code String} parameter would
 * admit the filter text through the audit boundary; this type has nowhere to put it, so the
 * boundary rule in {@code ArchitectureTest} holds for the shape as it does for every other
 * field.
 *
 * <p>{@link #render()} is the stored form: {@code userName eq ? and emails[type eq ?]}. Every
 * literal becomes {@code ?}, whatever its type, so even {@code active eq true} does not record
 * which way the probe went.
 */
public sealed interface AuditFilterShape {

    /** The stored form: canonical paths, lower-case operators, {@code ?} for every value. */
    String render();

    /** A filterable attribute path, as the audit trail names it. */
    enum Attribute {
        ID("id"),
        EXTERNAL_ID("externalId"),
        USER_NAME("userName"),
        NAME("name"),
        NAME_FORMATTED("name.formatted"),
        NAME_FAMILY_NAME("name.familyName"),
        NAME_GIVEN_NAME("name.givenName"),
        NAME_MIDDLE_NAME("name.middleName"),
        NAME_HONORIFIC_PREFIX("name.honorificPrefix"),
        NAME_HONORIFIC_SUFFIX("name.honorificSuffix"),
        DISPLAY_NAME("displayName"),
        PREFERRED_LANGUAGE("preferredLanguage"),
        LOCALE("locale"),
        TIMEZONE("timezone"),
        ACTIVE("active"),
        EMAILS("emails"),
        EMAILS_VALUE("emails.value"),
        EMAILS_TYPE("emails.type"),
        EMAILS_PRIMARY("emails.primary"),
        GROUPS("groups"),
        GROUPS_VALUE("groups.value"),
        GROUPS_DISPLAY("groups.display"),
        GROUPS_REF("groups.$ref"),
        GROUPS_TYPE("groups.type"),
        MEMBERS("members"),
        MEMBERS_VALUE("members.value"),
        MEMBERS_DISPLAY("members.display"),
        MEMBERS_REF("members.$ref"),
        MEMBERS_TYPE("members.type"),
        META("meta"),
        META_RESOURCE_TYPE("meta.resourceType"),
        META_CREATED("meta.created"),
        META_LAST_MODIFIED("meta.lastModified"),
        META_LOCATION("meta.location"),
        META_VERSION("meta.version");

        private final String path;

        Attribute(String path) {
            this.path = path;
        }

        /** The canonical dotted path. */
        public String path() {
            return path;
        }

        /** The last segment of the path, as a sub-attribute is written inside a value path. */
        String subAttributeName() {
            // With no dot, indexOf is -1 and the substring is the whole path.
            return path.substring(path.indexOf('.') + 1);
        }
    }

    /** A comparison operator. */
    enum Operator {
        EQ, NE, CO, SW, EW, GT, GE, LT, LE;

        String token() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** {@code attr op ?}. */
    record Comparison(Attribute attribute, Operator operator, boolean inValuePath)
            implements AuditFilterShape {

        @Override
        public String render() {
            return name(attribute, inValuePath) + " " + operator.token() + " ?";
        }
    }

    /** {@code attr pr}. */
    record Presence(Attribute attribute, boolean inValuePath) implements AuditFilterShape {

        @Override
        public String render() {
            return name(attribute, inValuePath) + " pr";
        }
    }

    /** {@code left and right}. */
    record And(AuditFilterShape left, AuditFilterShape right) implements AuditFilterShape {

        @Override
        public String render() {
            return "(" + left.render() + " and " + right.render() + ")";
        }
    }

    /** {@code left or right}. */
    record Or(AuditFilterShape left, AuditFilterShape right) implements AuditFilterShape {

        @Override
        public String render() {
            return "(" + left.render() + " or " + right.render() + ")";
        }
    }

    /** {@code not (inner)}. */
    record Not(AuditFilterShape inner) implements AuditFilterShape {

        @Override
        public String render() {
            return "not (" + inner.render() + ")";
        }
    }

    /** {@code attr[inner]}. */
    record ValuePath(Attribute attribute, AuditFilterShape inner) implements AuditFilterShape {

        @Override
        public String render() {
            return attribute.path() + "[" + inner.render() + "]";
        }
    }

    private static String name(Attribute attribute, boolean inValuePath) {
        return inValuePath ? attribute.subAttributeName() : attribute.path();
    }
}
