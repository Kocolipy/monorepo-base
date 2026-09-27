package com.example.backend.scim.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * RFC 7644 §3.9 attribute projection: which attributes of a rendered resource the caller
 * asked to see, or asked not to.
 *
 * <p>Applied to the rendered document rather than to the resource, which is what makes
 * the password's exemption structural instead of a special case. {@code password} is
 * declared {@code returned=never}, so it is never in the document this filters; a
 * request for {@code attributes=password} therefore selects nothing and yields the
 * always-returned attributes, with no branch here mentioning credentials at all. A
 * projection that worked on the domain object would need such a branch, and a branch is
 * something a later edit can lose.
 *
 * <p>Paths are validated against the vocabulary of the resource being projected, so naming
 * an attribute this service does not implement is a {@code 400 invalidValue} rather than a
 * resource that silently came back without it — the caller would otherwise read the
 * omission as "this resource has no such value".
 *
 * <p>Which vocabulary applies is chosen by the caller, through {@link #ofUser} or
 * {@link #ofGroup}, rather than inferred from the document afterwards. That is deliberate:
 * {@code members} is an attribute of a Group and not of a User, so a single shared
 * vocabulary would accept {@code attributes=members} on a User and return a resource
 * missing an attribute it never had.
 */
final class ScimAttributeProjection {

    /**
     * The attribute vocabulary of one resource type: the schema URI a fully-qualified path may
     * carry, the names a projection may name, and the declared attributes sub-paths are checked
     * against.
     *
     * <p>An enum rather than two copies of this class, because every rule below is identical for
     * the two resource types and only the vocabulary differs — and a second copy is where the two
     * would drift.
     */
    private enum Kind {

        USER(ScimSchemas.USER, "User"),
        GROUP(ScimSchemas.GROUP, "Group");

        private final String schemaPrefix;

        private final String label;

        Kind(String schemaUri, String label) {
            this.schemaPrefix = schemaUri + ":";
            this.label = label;
        }

        Set<String> projectableNames() {
            return this == USER
                    ? ScimUserAttributes.projectableNames()
                    : ScimGroupAttributes.projectableNames();
        }

        List<ScimUserAttributes.Attribute> schemaAttributes() {
            return this == USER
                    ? ScimUserAttributes.SCHEMA_ATTRIBUTES
                    : ScimGroupAttributes.SCHEMA_ATTRIBUTES;
        }

        /**
         * Attributes a projection may not remove.
         *
         * <p>The same set for both types as things stand — {@code schemas} and {@code id}, which
         * RFC 7643 §3.1 declares {@code returned=always} — because neither schema declares an
         * always-returned attribute of its own. Asked per kind anyway, so a future one is honoured
         * without a caller having to notice.
         */
        Set<String> alwaysReturned() {
            return ScimUserAttributes.alwaysReturned();
        }
    }

    /** No projection: the document is rendered whole. */
    static final ScimAttributeProjection NONE =
            new ScimAttributeProjection(Kind.USER, Map.of(), Map.of());

    private final Kind kind;

    /** Requested attribute -> requested sub-attributes, empty meaning the whole value. */
    private final Map<String, Set<String>> included;

    private final Map<String, Set<String>> excluded;

    private ScimAttributeProjection(
            Kind kind, Map<String, Set<String>> included, Map<String, Set<String>> excluded) {
        this.kind = kind;
        this.included = included;
        this.excluded = excluded;
    }

    /**
     * The projection the two mutually exclusive parameters describe, over the User vocabulary.
     *
     * @throws ScimErrorException {@code 400 invalidValue} when both are supplied, or when
     *                            either names something that is not an implemented
     *                            attribute
     */
    static ScimAttributeProjection ofUser(String attributes, String excludedAttributes) {
        return of(Kind.USER, attributes, excludedAttributes);
    }

    /** The same, over the Group vocabulary. */
    static ScimAttributeProjection ofGroup(String attributes, String excludedAttributes) {
        return of(Kind.GROUP, attributes, excludedAttributes);
    }

    private static ScimAttributeProjection of(
            Kind kind, String attributes, String excludedAttributes) {
        boolean hasIncluded = isPresent(attributes);
        boolean hasExcluded = isPresent(excludedAttributes);
        if (hasIncluded && hasExcluded) {
            throw ScimErrorException.invalidValue(
                    "attributes and excludedAttributes are mutually exclusive.");
        }
        if (hasIncluded) {
            return new ScimAttributeProjection(kind, parse(kind, attributes), Map.of());
        }
        if (hasExcluded) {
            return new ScimAttributeProjection(kind, Map.of(), parse(kind, excludedAttributes));
        }
        return NONE;
    }

    /** The rendered document with the projection applied. */
    Map<String, Object> apply(Map<String, Object> rendered) {
        if (included.isEmpty() && excluded.isEmpty()) {
            return rendered;
        }
        Map<String, Object> projected = new LinkedHashMap<>();
        for (Map.Entry<String, Object> attribute : rendered.entrySet()) {
            String name = attribute.getKey();
            if (kind.alwaysReturned().contains(name)) {
                projected.put(name, attribute.getValue());
                continue;
            }
            if (!included.isEmpty()) {
                Set<String> requestedSub = included.get(name);
                if (requestedSub != null) {
                    projected.put(name, restrictedTo(attribute.getValue(), requestedSub));
                }
                continue;
            }
            Set<String> removedSub = excluded.get(name);
            if (removedSub == null) {
                projected.put(name, attribute.getValue());
            } else if (!removedSub.isEmpty()) {
                projected.put(name, without(attribute.getValue(), removedSub));
            }
        }
        return projected;
    }

    /**
     * A complex value narrowed to the requested sub-attributes, or the value unchanged
     * when the whole attribute was asked for.
     */
    private static Object restrictedTo(Object value, Set<String> requestedSub) {
        if (requestedSub.isEmpty()) {
            return value;
        }
        return mapComplex(value, complex -> {
            Map<String, Object> narrowed = new LinkedHashMap<>();
            complex.forEach((key, sub) -> {
                if (requestedSub.contains(key)) {
                    narrowed.put(key, sub);
                }
            });
            return narrowed;
        });
    }

    /** A complex value with the named sub-attributes removed. */
    private static Object without(Object value, Set<String> removedSub) {
        return mapComplex(value, complex -> {
            Map<String, Object> remaining = new LinkedHashMap<>(complex);
            removedSub.forEach(remaining::remove);
            return remaining;
        });
    }

    /**
     * Applies a sub-attribute transform to a complex value, whether it is one object or
     * a multi-valued list of them; anything else is returned untouched, because a
     * sub-path against a simple value was already refused at parse time.
     */
    @SuppressWarnings("unchecked")
    private static Object mapComplex(
            Object value, UnaryOperator<Map<String, Object>> transform) {
        if (value instanceof Map<?, ?> complex) {
            return transform.apply((Map<String, Object>) complex);
        }
        if (value instanceof List<?> values) {
            List<Object> mapped = new ArrayList<>(values.size());
            for (Object element : values) {
                mapped.add(element instanceof Map<?, ?> complex
                        ? transform.apply((Map<String, Object>) complex)
                        : element);
            }
            return List.copyOf(mapped);
        }
        return value;
    }

    /**
     * The comma-separated parameter as attribute names and their requested
     * sub-attributes.
     *
     * <p>Names are matched case-insensitively, as RFC 7644 §3.10 requires of attribute
     * names, and resolved back to their canonical spelling so the rendered document's
     * keys are the ones being compared.
     */
    private static Map<String, Set<String>> parse(Kind kind, String parameter) {
        Map<String, Set<String>> paths = new LinkedHashMap<>();
        for (String raw : parameter.split(",")) {
            String path = unqualified(kind, raw.trim());
            if (path.isEmpty()) {
                continue;
            }
            int dot = path.indexOf('.');
            String top = canonicalTopLevel(kind, dot < 0 ? path : path.substring(0, dot));
            Set<String> sub = paths.computeIfAbsent(top, name -> new LinkedHashSet<>());
            if (dot >= 0) {
                sub.add(canonicalSubAttribute(kind, top, path.substring(dot + 1)));
            }
        }
        return Map.copyOf(paths);
    }

    /** The path with a leading schema URI for this resource type removed, if it carried one. */
    private static String unqualified(Kind kind, String path) {
        return path.regionMatches(true, 0, kind.schemaPrefix, 0, kind.schemaPrefix.length())
                ? path.substring(kind.schemaPrefix.length())
                : path;
    }

    private static String canonicalTopLevel(Kind kind, String name) {
        return kind.projectableNames().stream()
                .filter(known -> known.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> ScimErrorException.invalidValue(
                        "Not an attribute of this service's " + kind.label + " schema: "
                                + name.toLowerCase(Locale.ROOT)));
    }

    private static String canonicalSubAttribute(Kind kind, String top, String sub) {
        return subAttributeNames(kind, top).stream()
                .filter(known -> known.equalsIgnoreCase(sub))
                .findFirst()
                .orElseThrow(() -> ScimErrorException.invalidValue(
                        "Not a sub-attribute of " + top + ": " + sub.toLowerCase(Locale.ROOT)));
    }

    private static Set<String> subAttributeNames(Kind kind, String top) {
        Optional<ScimUserAttributes.Attribute> declared = kind.schemaAttributes().stream()
                .filter(attribute -> attribute.name().equals(top))
                .findFirst();
        return declared.map(attribute -> attribute.subAttributes().stream()
                        .map(ScimUserAttributes.Attribute::name)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private static boolean isPresent(String parameter) {
        return parameter != null && !parameter.isBlank();
    }
}
