package com.example.backend.scim.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
 * <p>Paths are validated against {@link ScimUserAttributes}, so naming an attribute this
 * service does not implement is a {@code 400 invalidValue} rather than a resource that
 * silently came back without it — the caller would otherwise read the omission as "this
 * User has no such value".
 */
final class ScimAttributeProjection {

    /** The schema prefix a fully-qualified attribute path may carry. */
    private static final String USER_PREFIX = ScimSchemas.USER + ":";

    /** No projection: the document is rendered whole. */
    static final ScimAttributeProjection NONE =
            new ScimAttributeProjection(Map.of(), Map.of());

    /** Requested attribute -> requested sub-attributes, empty meaning the whole value. */
    private final Map<String, Set<String>> included;

    private final Map<String, Set<String>> excluded;

    private ScimAttributeProjection(
            Map<String, Set<String>> included, Map<String, Set<String>> excluded) {
        this.included = included;
        this.excluded = excluded;
    }

    /**
     * The projection the two mutually exclusive parameters describe.
     *
     * @throws ScimErrorException {@code 400 invalidValue} when both are supplied, or when
     *                            either names something that is not an implemented
     *                            attribute
     */
    static ScimAttributeProjection of(String attributes, String excludedAttributes) {
        boolean hasIncluded = isPresent(attributes);
        boolean hasExcluded = isPresent(excludedAttributes);
        if (hasIncluded && hasExcluded) {
            throw ScimErrorException.invalidValue(
                    "attributes and excludedAttributes are mutually exclusive.");
        }
        if (hasIncluded) {
            return new ScimAttributeProjection(parse(attributes), Map.of());
        }
        if (hasExcluded) {
            return new ScimAttributeProjection(Map.of(), parse(excludedAttributes));
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
            if (ScimUserAttributes.alwaysReturned().contains(name)) {
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
            Object value, java.util.function.UnaryOperator<Map<String, Object>> transform) {
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
    private static Map<String, Set<String>> parse(String parameter) {
        Map<String, Set<String>> paths = new LinkedHashMap<>();
        for (String raw : parameter.split(",")) {
            String path = unqualified(raw.trim());
            if (path.isEmpty()) {
                continue;
            }
            int dot = path.indexOf('.');
            String top = canonicalTopLevel(dot < 0 ? path : path.substring(0, dot));
            Set<String> sub = paths.computeIfAbsent(top, name -> new LinkedHashSet<>());
            if (dot >= 0) {
                sub.add(canonicalSubAttribute(top, path.substring(dot + 1)));
            }
        }
        return Map.copyOf(paths);
    }

    /** The path with a leading core-User schema URI removed, if it carried one. */
    private static String unqualified(String path) {
        return path.regionMatches(true, 0, USER_PREFIX, 0, USER_PREFIX.length())
                ? path.substring(USER_PREFIX.length())
                : path;
    }

    private static String canonicalTopLevel(String name) {
        return ScimUserAttributes.projectableNames().stream()
                .filter(known -> known.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> ScimErrorException.invalidValue(
                        "Not an attribute of this service's User schema: "
                                + name.toLowerCase(Locale.ROOT)));
    }

    private static String canonicalSubAttribute(String top, String sub) {
        return subAttributeNames(top).stream()
                .filter(known -> known.equalsIgnoreCase(sub))
                .findFirst()
                .orElseThrow(() -> ScimErrorException.invalidValue(
                        "Not a sub-attribute of " + top + ": " + sub.toLowerCase(Locale.ROOT)));
    }

    private static Set<String> subAttributeNames(String top) {
        Optional<ScimUserAttributes.Attribute> declared = ScimUserAttributes.SCHEMA_ATTRIBUTES
                .stream()
                .filter(attribute -> attribute.name().equals(top))
                .findFirst();
        return declared.map(attribute -> attribute.subAttributes().stream()
                        .map(ScimUserAttributes.Attribute::name)
                        .collect(java.util.stream.Collectors.toSet()))
                .orElse(Set.of());
    }

    private static boolean isPresent(String parameter) {
        return parameter != null && !parameter.isBlank();
    }
}
