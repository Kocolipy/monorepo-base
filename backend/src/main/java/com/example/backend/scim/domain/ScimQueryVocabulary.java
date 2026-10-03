package com.example.backend.scim.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which attribute paths one resource type can be queried by, and how each compares.
 *
 * <p>Derived, not declared: each path's type, case sensitivity and cardinality are read off
 * {@link ScimResourceSchema}, the same definition {@code /Schemas} renders. A connector reads
 * {@code caseExact} from discovery and builds its filters on it, so a filter that compared
 * differently from what discovery says would return a different set than the one the connector
 * asked for — and deriving both from one definition is what makes that impossible rather than
 * merely tested. Every profile string is case-insensitive, as RFC 7643 §2.2 and §8.7.1 make it;
 * only identifiers are case-exact — the common {@code id} and {@code externalId}, the
 * {@code meta} values, and the {@code value} and {@code $ref} of a membership.
 *
 * <p>What this class still owns is the one rule that is the query protocol's own: a path is
 * queryable when {@link ScimFilterPath} names it, the resource type has the attribute, and the
 * attribute is returned. A sub-attribute inherits its parent's cardinality, because
 * {@code emails.value} holds as many values as {@code emails} does.
 *
 * <p>{@code password} is never returned, so it is absent from both vocabularies and no query can
 * select, sort or probe by it. {@code schemas} has no query path, so it is not queryable either.
 */
public final class ScimQueryVocabulary {

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
    public record Attribute(
            ScimFilterPath path, ScimAttributeType kind, boolean caseExact, boolean multiValued) {

        /** Whether this is a complex attribute, which a comparison must reach through a sub-path. */
        public boolean isComplex() {
            return kind == ScimAttributeType.COMPLEX;
        }

        /** Whether this compares as text: a string or a reference. */
        public boolean isTextual() {
            return kind == ScimAttributeType.STRING || kind == ScimAttributeType.REFERENCE;
        }
    }

    private static final ScimQueryVocabulary USER = derive(ScimResourceType.USER);

    private static final ScimQueryVocabulary GROUP = derive(ScimResourceType.GROUP);

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

    private static ScimQueryVocabulary derive(ScimResourceType type) {
        ScimResourceSchema schema = ScimResourceSchema.of(type);
        Map<ScimFilterPath, Attribute> derived = new EnumMap<>(ScimFilterPath.class);
        for (ScimFilterPath path : ScimFilterPath.values()) {
            queryable(schema, path).ifPresent(attribute -> derived.put(path, attribute));
        }
        return new ScimQueryVocabulary(derived);
    }

    /** The path's comparison, when this schema has a returned attribute there. */
    private static Optional<Attribute> queryable(ScimResourceSchema schema, ScimFilterPath path) {
        if (path.parent() == null) {
            return schema.find(path.attributeName())
                    .filter(ScimAttribute::isReturned)
                    .map(top -> new Attribute(path, top.type(), top.caseExact(), top.multiValued()));
        }
        return schema.find(path.parent().attributeName())
                .filter(ScimAttribute::isReturned)
                .flatMap(parent -> parent.subAttribute(path.attributeName())
                        .filter(ScimAttribute::isReturned)
                        .map(sub -> new Attribute(
                                path, sub.type(), sub.caseExact(), parent.multiValued())));
    }
}
