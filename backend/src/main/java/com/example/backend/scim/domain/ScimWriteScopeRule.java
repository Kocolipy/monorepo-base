package com.example.backend.scim.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Whether a SCIM request needs write scope, decided from the method and the path
 * alone.
 *
 * <p>Method alone is not enough, and that is the whole reason this is a class
 * rather than a line in a filter. SCIM's {@code .search} endpoints are
 * {@code POST} requests that read: a filter carrying personal values belongs in a
 * body rather than in a URL, so RFC 7644 makes the read a {@code POST}. A rule
 * that read "POST means write" would refuse a read-only connector the one endpoint
 * the standard added for its benefit.
 *
 * <p>Decided from the request line rather than from a handler annotation, because
 * the refusal has to happen in the filter: a read-only token must be turned away
 * before a mutating handler runs, not by a handler that remembered to check. That
 * makes the rule uniform over every present and future SCIM path — a new mutating
 * endpoint is covered the day it is added, with no scope check of its own.
 */
public final class ScimWriteScopeRule {

    /** Methods that cannot change a resource whatever the path is. */
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    /**
     * The suffix that makes a {@code POST} a read. Matched as a path suffix rather
     * than against a list of the three search paths, so {@code /Users/.search},
     * {@code /Groups/.search} and the base {@code /.search} are all covered, along
     * with any the resource types to come add.
     */
    private static final String SEARCH_SUFFIX = "/.search";

    private ScimWriteScopeRule() {
    }

    /**
     * Whether this request may proceed only with {@link ConnectorTokenScope#READ_WRITE}.
     *
     * @param method HTTP method, in any case
     * @param path   request path within the SCIM namespace
     */
    public static boolean requiresWriteScope(String method, String path) {
        if (method == null) {
            // Unclassifiable, so the stronger scope is required. The alternative
            // default would let anything this rule cannot read through as a read.
            return true;
        }
        if (READ_METHODS.contains(method.toUpperCase(Locale.ROOT))) {
            return false;
        }
        return path == null || !path.endsWith(SEARCH_SUFFIX);
    }
}
