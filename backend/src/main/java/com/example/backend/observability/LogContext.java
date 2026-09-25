package com.example.backend.observability;

import java.util.Set;
import org.slf4j.MDC;

/**
 * The only way anything in this service writes to the logging context.
 *
 * <p>Logs carry correlation ids and nothing else. A {@code userName}, a filter
 * expression, a password, a bearer value, a hash and a cookie value never reach a
 * log line — neither in a message nor as a context entry — because a log store is
 * read by more people, retained longer, and shipped further than the database the
 * value came from. That rule cannot be enforced by review of {@code MDC.put}
 * call sites scattered across the service, so there are none: this class exposes
 * three named setters and no general-purpose one, and
 * {@code ArchitectureTest.mdc_is_only_touched_by_the_log_context} holds it to
 * being the single class in production code that touches {@link MDC}.
 *
 * <p>The three keys are the ones the identity surface needs to correlate by, and
 * all three are ids rather than values:
 *
 * <ul>
 *   <li>{@link #REQUEST_ID} — minted per inbound request by
 *       {@link RequestIdFilter}, never taken from the caller.
 *   <li>{@link #CONNECTOR_ID} — which provisioning connector's token
 *       authenticated the request, once connectors exist.
 *   <li>{@link #RESOURCE_ID} — the stable id of the resource being acted on,
 *       which is why it is an id: the resource's readable identifiers are exactly
 *       what must not be logged.
 * </ul>
 *
 * <p>Every value is passed through {@link #sanitize} first. A value that reaches
 * here can still be client-influenced further out — a connector id read from a
 * token, a resource id parsed from a path — and a newline or a carriage return
 * inside one would let a caller close the current log record and forge the next
 * (CWE-117). Structured output makes that harder, not impossible: the sanitizing
 * is what makes it impossible.
 */
public final class LogContext {

    /** Correlation id for one inbound HTTP request. ECS {@code http.request.id}. */
    public static final String REQUEST_ID = "http.request.id";

    /** The provisioning connector whose token authenticated the request. */
    public static final String CONNECTOR_ID = "scim.connector.id";

    /** Stable id of the resource being read or written. Never its userName. */
    public static final String RESOURCE_ID = "scim.resource.id";

    /**
     * Every key this class will write, so {@link #clear()} can remove exactly
     * what was added and nothing a library put there.
     */
    private static final Set<String> KEYS = Set.of(REQUEST_ID, CONNECTOR_ID, RESOURCE_ID);

    /**
     * Longest value written. An id is far shorter than this; the cap exists so a
     * caller-supplied value cannot turn one log record into a megabyte.
     */
    private static final int MAX_VALUE_LENGTH = 128;

    private LogContext() {
    }

    /** Puts the request correlation id in scope for the current thread. */
    public static Scope requestId(String requestId) {
        return put(REQUEST_ID, requestId);
    }

    /** Puts the authenticated connector's id in scope for the current thread. */
    public static Scope connectorId(String connectorId) {
        return put(CONNECTOR_ID, connectorId);
    }

    /** Puts the acted-on resource's stable id in scope for the current thread. */
    public static Scope resourceId(String resourceId) {
        return put(RESOURCE_ID, resourceId);
    }

    /**
     * Removes every key this class owns, leaving anything else in the context
     * alone. The filter does this at the end of a request as a backstop; a
     * {@link Scope} closed normally has already undone its own entry.
     */
    public static void clear() {
        KEYS.forEach(MDC::remove);
    }

    /**
     * The value as it is safe to write: control characters — line breaks among
     * them — replaced by {@code _}, and the result truncated. {@code null} and
     * blank both become {@code null}, which means "do not record this key" rather
     * than recording an empty one, so a reader can tell an absent id from a
     * present empty string.
     */
    static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String truncated = value.length() > MAX_VALUE_LENGTH
                ? value.substring(0, MAX_VALUE_LENGTH)
                : value;
        StringBuilder safe = new StringBuilder(truncated.length());
        truncated.codePoints().forEach(codePoint ->
                safe.appendCodePoint(Character.isISOControl(codePoint) ? '_' : codePoint));
        return safe.toString();
    }

    private static Scope put(String key, String value) {
        String previous = MDC.get(key);
        String sanitized = sanitize(value);
        if (sanitized == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, sanitized);
        }
        return new Scope(key, previous);
    }

    /**
     * One context entry's lifetime. Closing it restores whatever the key held
     * before, so a nested scope — a resource id set inside a request — cannot
     * erase the outer one, and a scope that never nests simply removes its key.
     *
     * <p>Intended for try-with-resources. {@link AutoCloseable} rather than
     * {@code Closeable} because nothing here can fail with an
     * {@code IOException}, and a checked exception on close would spread through
     * every call site for no reason.
     */
    public static final class Scope implements AutoCloseable {

        private final String key;
        private final String previous;

        private Scope(String key, String previous) {
            this.key = key;
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, previous);
            }
        }
    }
}
