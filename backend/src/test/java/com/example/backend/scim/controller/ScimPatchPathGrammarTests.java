package com.example.backend.scim.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.scim.controller.ScimPatchPathGrammar.Path;
import com.example.backend.scim.domain.ScimEmailFilter;
import com.example.backend.scim.domain.ScimEmailFilter.Condition;
import com.example.backend.scim.domain.ScimEmailPart;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

/**
 * A PATCH path's text in, a parsed path or an email value filter out — or the exact refusal the
 * response carries. No PatchOp body is needed: each case is the text between the brackets.
 */
class ScimPatchPathGrammarTests {

    private static final String ONLY_EQ_AND =
            "Only 'sub-attribute eq value' comparisons joined by 'and' are supported in an emails"
                    + " filter.";

    private static final String NOT_A_LITERAL =
            "A compared value must be a JSON string, boolean or null.";

    private static final String GROUPING = "Grouping is not supported in an emails filter.";

    private static final String UNTERMINATED = "The emails filter has an unterminated string.";

    private static ScimEmailFilter filterOf(Condition... conditions) {
        return new ScimEmailFilter(List.of(conditions));
    }

    private static void refusedAs(String filter, String scimType, String detail) {
        assertThatThrownBy(() -> ScimPatchPathGrammar.emailFilter(filter))
                .as(filter)
                .isInstanceOfSatisfying(ScimErrorException.class, refusal -> {
                    assertThat(refusal.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(refusal.scimType()).isEqualTo(scimType);
                    assertThat(refusal.detail()).isEqualTo(detail);
                });
    }

    // ---- paths ----------------------------------------------------------------------------

    @Test
    void a_path_reads_its_attribute_filter_and_sub_attribute() {
        assertThat(ScimPatchPathGrammar.path("displayName"))
                .isEqualTo(new Path("displayname", null, null));
        assertThat(ScimPatchPathGrammar.path("Name.GivenName"))
                .isEqualTo(new Path("name", null, "givenname"));
        assertThat(ScimPatchPathGrammar.path("Emails[Type eq \"Work\"]"))
                .isEqualTo(new Path("emails", "Type eq \"Work\"", null));
        assertThat(ScimPatchPathGrammar.path("emails[type eq \"work\"].Value"))
                .isEqualTo(new Path("emails", "type eq \"work\"", "value"));
    }

    @Test
    void a_path_is_read_without_surrounding_whitespace_or_the_user_schema_prefix() {
        assertThat(ScimPatchPathGrammar.path("  locale \t"))
                .isEqualTo(new Path("locale", null, null));
        assertThat(ScimPatchPathGrammar.path(
                "urn:ietf:params:scim:schemas:core:2.0:User:name.familyName"))
                .isEqualTo(new Path("name", null, "familyname"));
        assertThat(ScimPatchPathGrammar.path(
                " URN:IETF:PARAMS:SCIM:SCHEMAS:CORE:2.0:USER:userName"))
                .isEqualTo(new Path("username", null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "emails..value", "emails[", "emails[]", "1userName",
            "name.givenName.x", "userName!", "emails[type eq \"work\"]x",
            "urn:ietf:params:scim:schemas:core:2.0:User:"})
    void text_that_is_not_an_attribute_path_is_invalid_path(String text) {
        assertThatThrownBy(() -> ScimPatchPathGrammar.path(text))
                .isInstanceOfSatisfying(ScimErrorException.class, refusal -> {
                    assertThat(refusal.scimType()).isEqualTo("invalidPath");
                    assertThat(refusal.detail()).isEqualTo("Not a valid attribute path: "
                            + ScimUserRequestReader.sanitized(text));
                });
    }

    @Test
    void only_a_bare_name_is_an_attribute_name() {
        assertThat(ScimPatchPathGrammar.isAttributeName("displayName")).isTrue();
        assertThat(ScimPatchPathGrammar.isAttributeName("x_$-9")).isTrue();
        assertThat(ScimPatchPathGrammar.isAttributeName("name.givenName")).isFalse();
        assertThat(ScimPatchPathGrammar.isAttributeName("emails[type eq \"w\"]")).isFalse();
        assertThat(ScimPatchPathGrammar.isAttributeName("1locale")).isFalse();
        assertThat(ScimPatchPathGrammar.isAttributeName("")).isFalse();
    }

    @Test
    void an_emails_sub_attribute_name_is_matched_case_insensitively() {
        assertThat(ScimPatchPathGrammar.emailPart("VALUE")).contains(ScimEmailPart.VALUE);
        assertThat(ScimPatchPathGrammar.emailPart("Type")).contains(ScimEmailPart.TYPE);
        assertThat(ScimPatchPathGrammar.emailPart("primary")).contains(ScimEmailPart.PRIMARY);
        assertThat(ScimPatchPathGrammar.emailPart("display")).isEmpty();
    }

    // ---- value filters: what is read ------------------------------------------------------

    @Test
    void one_comparison_reads_as_one_condition_of_the_sub_attributes_type() {
        assertThat(ScimPatchPathGrammar.emailFilter("type eq \"work\""))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.TYPE, "work")));
        assertThat(ScimPatchPathGrammar.emailFilter("value eq \"a@b.example\""))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.VALUE, "a@b.example")));
        assertThat(ScimPatchPathGrammar.emailFilter("primary eq true"))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.PRIMARY, true)));
        assertThat(ScimPatchPathGrammar.emailFilter("primary eq false"))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.PRIMARY, false)));
        assertThat(ScimPatchPathGrammar.emailFilter("type eq null"))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.TYPE, null)));
    }

    /** Sub-attribute and operator are case-insensitive; the compared value is kept as written. */
    @Test
    void names_and_operator_are_case_insensitive_and_surrounding_whitespace_is_ignored() {
        assertThat(ScimPatchPathGrammar.emailFilter("  TYPE   EQ   \"Work\"  "))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.TYPE, "Work")));
    }

    @Test
    void and_joins_comparisons_in_order_whatever_its_case_or_whitespace() {
        assertThat(ScimPatchPathGrammar.emailFilter(
                "value eq \"a@b.example\" AND primary eq true\tand\ttype eq null"))
                .isEqualTo(filterOf(
                        new Condition(ScimEmailPart.VALUE, "a@b.example"),
                        new Condition(ScimEmailPart.PRIMARY, true),
                        new Condition(ScimEmailPart.TYPE, null)));
    }

    @Test
    void and_inside_a_quoted_value_is_part_of_the_value() {
        assertThat(ScimPatchPathGrammar.emailFilter("value eq \"a and b@x.example\""))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.VALUE, "a and b@x.example")));
    }

    /** An escaped quote does not end the string, so an {@code and} after it is still inside. */
    @Test
    void an_escaped_quote_does_not_end_the_quoted_value() {
        assertThat(ScimPatchPathGrammar.emailFilter("value eq \"quote\\\" and x\""))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.VALUE, "quote\" and x")));
        assertThat(ScimPatchPathGrammar.emailFilter("value eq \"back\\\\\" and type eq \"w\""))
                .isEqualTo(filterOf(
                        new Condition(ScimEmailPart.VALUE, "back\\"),
                        new Condition(ScimEmailPart.TYPE, "w")));
    }

    /** Grouping characters inside a quoted value are part of the value, not grouping. */
    @Test
    void brackets_inside_a_quoted_value_are_not_grouping() {
        assertThat(ScimPatchPathGrammar.emailFilter("type eq \"(w[o]rk)\""))
                .isEqualTo(filterOf(new Condition(ScimEmailPart.TYPE, "(w[o]rk)")));
    }

    // ---- value filters: what is refused ---------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "type", "type eq",
            "type eq \"work\" and and type eq \"home\""})
    void empty_or_incomplete_input_is_not_a_comparison(String filter) {
        refusedAs(filter, "invalidFilter", ONLY_EQ_AND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"label eq \"x\"", "display eq \"x\"", "type.sub eq \"x\""})
    void an_attribute_emails_does_not_have_is_unsupported(String filter) {
        refusedAs(filter, "invalidFilter",
                filter.startsWith("type.") ? ONLY_EQ_AND : "emails has no such sub-attribute.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"type co \"wo\"", "type ne \"work\"", "type sw \"w\"",
            "type gt \"w\"", "primary is true"})
    void any_operator_but_eq_is_unsupported(String filter) {
        refusedAs(filter, "invalidFilter",
                "Only the eq operator is supported in an emails filter.");
    }

    @Test
    void a_unary_operator_is_unsupported() {
        refusedAs("type pr", "invalidFilter", ONLY_EQ_AND);
    }

    /**
     * {@code or} is not split on, and neither is an {@code and} with no comparison after it, so
     * the trailing text joins the value, which is then not one literal.
     */
    @ParameterizedTest
    @ValueSource(strings = {"type eq \"work\" or type eq \"home\"", "type eq work",
            "type eq \"work\" and", "type eq \"work\" xyz value eq \"x\"",
            "primary eq truexand value eq \"x\"", "type eq \"work\" andvalue eq \"x\""})
    void text_that_is_not_one_json_literal_is_refused(String filter) {
        refusedAs(filter, "invalidFilter", NOT_A_LITERAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"primary eq \"true\"", "primary eq null", "primary eq 1"})
    void primary_compares_only_with_a_boolean(String filter) {
        refusedAs(filter, "invalidFilter", "primary compares with true or false.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"type eq 7", "value eq true", "type eq {}"})
    void value_and_type_compare_only_with_a_string_or_null(String filter) {
        refusedAs(filter, "invalidFilter", "value and type compare with a quoted string.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"(type eq \"work\")", "type eq \"work\")", "[type eq \"work\"]",
            "(type eq \"work\"", "not (type eq \"work\")", "type eq \"work\" and [value eq \"x\""})
    void grouping_is_refused_as_grouping(String filter) {
        refusedAs(filter, "invalidFilter", GROUPING);
    }

    /** An unterminated string is refused as a malformed path, as it always has been. */
    @ParameterizedTest
    @ValueSource(strings = {"type eq \"work", "value eq \"abc\\", "value eq \"a\\\""})
    void an_unterminated_string_is_invalid_path(String filter) {
        refusedAs(filter, "invalidPath", UNTERMINATED);
    }
}
