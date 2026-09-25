package com.example.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * The logging context's two guarantees: a value cannot forge a log record, and a
 * scope leaves the context exactly as it found it.
 *
 * <p>Both matter for the same reason. The context is the only structured channel
 * the identity surface writes correlation ids on, so an id that leaks past the
 * request that owned it attaches the wrong record to the wrong exchange, and an
 * id carrying a line break turns one record into two — the second of which the
 * caller wrote.
 */
class LogContextTests {

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void aRequestIdIsVisibleToTheLoggerWhileItsScopeIsOpen() {
        try (LogContext.Scope scope = LogContext.requestId("abc-123")) {
            assertThat(MDC.get(LogContext.REQUEST_ID)).isEqualTo("abc-123");
        }

        assertThat(MDC.get(LogContext.REQUEST_ID)).isNull();
    }

    @Test
    void theThreeKeysAreIndependent() {
        try (LogContext.Scope request = LogContext.requestId("r-1");
                LogContext.Scope connector = LogContext.connectorId("c-1");
                LogContext.Scope resource = LogContext.resourceId("u-1")) {
            assertThat(MDC.get(LogContext.REQUEST_ID)).isEqualTo("r-1");
            assertThat(MDC.get(LogContext.CONNECTOR_ID)).isEqualTo("c-1");
            assertThat(MDC.get(LogContext.RESOURCE_ID)).isEqualTo("u-1");
        }

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    /**
     * A resource id set for one operation inside a longer-lived one must not
     * destroy the outer value on the way out — otherwise the records after it in
     * the same request would be attributed to no resource at all.
     */
    @Test
    void closingANestedScopeRestoresTheValueItShadowed() {
        try (LogContext.Scope outer = LogContext.resourceId("outer")) {
            try (LogContext.Scope inner = LogContext.resourceId("inner")) {
                assertThat(MDC.get(LogContext.RESOURCE_ID)).isEqualTo("inner");
            }
            assertThat(MDC.get(LogContext.RESOURCE_ID)).isEqualTo("outer");
        }

        assertThat(MDC.get(LogContext.RESOURCE_ID)).isNull();
    }

    /**
     * The log-forging case (CWE-117). A newline in a value that reached the
     * context from a token or a path would otherwise end the current record and
     * begin one of the caller's choosing.
     */
    @Test
    void controlCharactersAreReplacedSoAValueCannotEndTheRecord() {
        try (LogContext.Scope scope =
                LogContext.connectorId("real\n{\"log\":{\"level\":\"INFO\"}}\r\tend")) {
            String recorded = MDC.get(LogContext.CONNECTOR_ID);

            assertThat(recorded).doesNotContain("\n", "\r", "\t");
            assertThat(recorded).isEqualTo("real_{\"log\":{\"level\":\"INFO\"}}__end");
        }
    }

    @Test
    void aValueIsTruncatedSoOneRecordCannotBeMadeUnbounded() {
        String overlong = "x".repeat(500);

        try (LogContext.Scope scope = LogContext.resourceId(overlong)) {
            assertThat(MDC.get(LogContext.RESOURCE_ID)).hasSize(128);
        }
    }

    /**
     * Absent rather than empty: a reader can then tell "no connector
     * authenticated this" from "a connector with an empty id did", which an empty
     * string would make indistinguishable.
     *
     * <p>Asserted on the context map rather than only on {@code MDC.get}, because
     * a key present with a null value reads the same through {@code get} and
     * differently through the encoder — an ECS record would carry the field with a
     * null, which is the third state this is meant to rule out.
     */
    @Test
    void aNullOrBlankValueRecordsNothingAtAll() {
        try (LogContext.Scope nothing = LogContext.connectorId(null);
                LogContext.Scope blank = LogContext.resourceId("   ")) {
            assertThat(MDC.get(LogContext.CONNECTOR_ID)).isNull();
            assertThat(MDC.get(LogContext.RESOURCE_ID)).isNull();
            // An empty context is reported as a null map by some MDC adapters, which
            // is the same statement as an empty one for this assertion's purpose.
            assertThat(Optional.ofNullable(MDC.getCopyOfContextMap()).orElseGet(Map::of))
                    .doesNotContainKey(LogContext.CONNECTOR_ID)
                    .doesNotContainKey(LogContext.RESOURCE_ID);
        }
    }

    /**
     * A blank value must not silently erase an id already in scope — and must
     * still restore it on the way out.
     */
    @Test
    void aBlankValueRemovesTheKeyForItsScopeAndRestoresTheOuterValue() {
        try (LogContext.Scope outer = LogContext.requestId("r-1")) {
            try (LogContext.Scope blank = LogContext.requestId("")) {
                assertThat(MDC.get(LogContext.REQUEST_ID)).isNull();
            }
            assertThat(MDC.get(LogContext.REQUEST_ID)).isEqualTo("r-1");
        }
    }

    @Test
    void clearRemovesEveryKeyThisClassOwnsAndNothingElse() {
        MDC.put("library.key", "left alone");
        LogContext.requestId("r-1");
        LogContext.connectorId("c-1");
        LogContext.resourceId("u-1");

        LogContext.clear();

        assertThat(MDC.get(LogContext.REQUEST_ID)).isNull();
        assertThat(MDC.get(LogContext.CONNECTOR_ID)).isNull();
        assertThat(MDC.get(LogContext.RESOURCE_ID)).isNull();
        assertThat(MDC.get("library.key")).isEqualTo("left alone");
    }
}
