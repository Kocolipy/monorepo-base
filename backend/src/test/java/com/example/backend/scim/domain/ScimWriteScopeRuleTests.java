package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Which SCIM requests a read-only connector may make.
 *
 * <p>This is the unit test the ticket asks for in place of an end-to-end proof: there
 * is no mutating SCIM endpoint yet, so the scope check's correctness is established
 * here and the end-to-end refusal is carried by the User-creation ticket. What is
 * proved here is the decision itself, over the request lines that will exist —
 * including the case a naive "POST means write" rule gets wrong.
 */
class ScimWriteScopeRuleTests {

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void a_reading_method_never_needs_write_scope(String method) {
        assertThat(ScimWriteScopeRule.requiresWriteScope(method, "/scim/v2/Users"))
                .isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "POST,/scim/v2/Users",
        "POST,/scim/v2/Groups",
        "PUT,/scim/v2/Users/8f14e45f-ceea-467a-9575-28a1b0e0e8a1",
        "PATCH,/scim/v2/Users/8f14e45f-ceea-467a-9575-28a1b0e0e8a1",
        "DELETE,/scim/v2/Users/8f14e45f-ceea-467a-9575-28a1b0e0e8a1",
        "DELETE,/scim/v2/Groups/8f14e45f-ceea-467a-9575-28a1b0e0e8a1",
    })
    void a_mutating_request_needs_write_scope(String method, String path) {
        assertThat(ScimWriteScopeRule.requiresWriteScope(method, path)).isTrue();
    }

    /**
     * The case that makes the rule worth having. RFC 7644 makes search a POST so a
     * filter carrying personal values need not go in a URL; a read-only connector is
     * the caller that most wants it.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "/scim/v2/Users/.search",
        "/scim/v2/Groups/.search",
        "/scim/v2/.search",
    })
    void a_search_post_is_a_read(String path) {
        assertThat(ScimWriteScopeRule.requiresWriteScope("POST", path)).isFalse();
    }

    /**
     * Only the last segment being {@code .search} makes it a search. A resource whose
     * id happened to contain the word must not become a read.
     */
    @Test
    void a_path_merely_containing_search_is_still_a_write() {
        assertThat(ScimWriteScopeRule.requiresWriteScope(
                        "POST", "/scim/v2/Users/.search/extra"))
                .isTrue();
    }

    @Test
    void the_method_is_read_case_insensitively() {
        assertThat(ScimWriteScopeRule.requiresWriteScope("get", "/scim/v2/Users")).isFalse();
        assertThat(ScimWriteScopeRule.requiresWriteScope("post", "/scim/v2/Users")).isTrue();
    }

    /**
     * An unknown method is treated as a write. The default has to be the restrictive
     * one: a method this rule has never heard of is not evidence that it is safe.
     */
    @Test
    void an_unrecognised_method_is_treated_as_a_write() {
        assertThat(ScimWriteScopeRule.requiresWriteScope("PROPFIND", "/scim/v2/Users"))
                .isTrue();
    }

    /**
     * An unclassifiable request line requires the stronger scope. Neither a missing
     * method nor a missing path is evidence that the request is a read.
     */
    @Test
    void an_unclassifiable_request_line_requires_write_scope() {
        assertThat(ScimWriteScopeRule.requiresWriteScope(null, "/scim/v2/Users")).isTrue();
        assertThat(ScimWriteScopeRule.requiresWriteScope("POST", null)).isTrue();
    }

    @Test
    void write_scope_permits_a_write_and_read_only_does_not() {
        assertThat(ConnectorTokenScope.READ_WRITE.permitsWrite()).isTrue();
        assertThat(ConnectorTokenScope.READ_ONLY.permitsWrite()).isFalse();
    }
}
