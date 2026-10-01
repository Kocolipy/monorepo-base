package com.example.backend.scim.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.scim.domain.ScimEmail;
import com.example.backend.scim.domain.ScimEmailFilter;
import com.example.backend.scim.domain.ScimEmailFilter.Condition;
import com.example.backend.scim.domain.ScimEmailPart;
import com.example.backend.scim.domain.ScimName;
import com.example.backend.scim.domain.ScimUserPatchOperation;
import com.example.backend.scim.domain.ScimUserPatchOperation.AddEmails;
import com.example.backend.scim.domain.ScimUserPatchOperation.EmailUpdate;
import com.example.backend.scim.domain.ScimUserPatchOperation.MergeName;
import com.example.backend.scim.domain.ScimUserPatchOperation.NamePart;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveEmailPart;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveEmails;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveExternalId;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveName;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveNamePart;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemovePassword;
import com.example.backend.scim.domain.ScimUserPatchOperation.RemoveText;
import com.example.backend.scim.domain.ScimUserPatchOperation.ReplaceEmails;
import com.example.backend.scim.domain.ScimUserPatchOperation.SetActive;
import com.example.backend.scim.domain.ScimUserPatchOperation.SetExternalId;
import com.example.backend.scim.domain.ScimUserPatchOperation.SetPassword;
import com.example.backend.scim.domain.ScimUserPatchOperation.SetText;
import com.example.backend.scim.domain.ScimUserPatchOperation.TextAttribute;
import com.example.backend.scim.domain.ScimUserPatchOperation.UpdateEmails;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

/** A PATCH body read into operations, and every refusal that depends on the request alone. */
class ScimUserPatchReaderTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static List<ScimUserPatchOperation> read(String operations) {
        return ScimUserPatchReader.readPatch(JSON.readTree("""
                {"schemas":["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                 "Operations":%s}""".formatted(operations)));
    }

    private static ScimUserPatchOperation one(String op, String path, String value) {
        String pathPart = path == null ? "" : ",\"path\":" + JSON.writeValueAsString(path);
        String valuePart = value == null ? "" : ",\"value\":" + value;
        List<ScimUserPatchOperation> read =
                read("[{\"op\":\"" + op + "\"" + pathPart + valuePart + "}]");
        assertThat(read).hasSize(1);
        return read.get(0);
    }

    private static void refused(String operations, int status, String scimType) {
        assertThatThrownBy(() -> read(operations))
                .isInstanceOfSatisfying(ScimErrorException.class, refusal -> {
                    assertThat(refusal.status().value()).isEqualTo(status);
                    assertThat(refusal.scimType()).isEqualTo(scimType);
                });
    }

    private static ScimEmailFilter typeIs(String type) {
        return new ScimEmailFilter(List.of(new Condition(ScimEmailPart.TYPE, type)));
    }

    // ---- the envelope ---------------------------------------------------------------------

    @Test
    void a_body_that_is_not_a_patch_op_is_invalid_syntax() {
        for (String body : List.of(
                "[]",
                "{\"Operations\":[{\"op\":\"remove\",\"path\":\"displayName\"}]}",
                "{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:User\"],"
                        + "\"Operations\":[{\"op\":\"remove\",\"path\":\"displayName\"}]}",
                "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"]}",
                "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                        + "\"Operations\":[]}",
                "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:PatchOp\"],"
                        + "\"Operations\":[\"remove\"]}")) {
            assertThatThrownBy(() -> ScimUserPatchReader.readPatch(JSON.readTree(body)))
                    .as(body)
                    .isInstanceOfSatisfying(ScimErrorException.class, refusal ->
                            assertThat(refusal.scimType()).isEqualTo("invalidSyntax"));
        }
        assertThatThrownBy(() -> ScimUserPatchReader.readPatch(null))
                .isInstanceOf(ScimErrorException.class);
    }

    @Test
    void an_unknown_or_missing_op_is_invalid_syntax_and_op_is_case_insensitive() {
        refused("[{\"op\":\"move\",\"path\":\"displayName\"}]", 400, "invalidSyntax");
        refused("[{\"path\":\"displayName\"}]", 400, "invalidSyntax");
        assertThat(one("Replace", "displayName", "\"Ada\""))
                .isEqualTo(new SetText(TextAttribute.DISPLAY_NAME, "Ada"));
        assertThat(one("REMOVE", "displayName", null))
                .isEqualTo(new RemoveText(TextAttribute.DISPLAY_NAME));
        assertThat(one("aDd", "displayName", "\"Ada\""))
                .isEqualTo(new SetText(TextAttribute.DISPLAY_NAME, "Ada"));
    }

    // ---- paths ----------------------------------------------------------------------------

    @Test
    void every_single_valued_attribute_reads_case_insensitively_and_with_the_schema_prefix() {
        assertThat(one("replace", "USERNAME", "\"ada\""))
                .isEqualTo(new SetText(TextAttribute.USER_NAME, "ada"));
        assertThat(one("replace",
                "urn:ietf:params:scim:schemas:core:2.0:User:displayName", "\"Ada\""))
                .isEqualTo(new SetText(TextAttribute.DISPLAY_NAME, "Ada"));
        assertThat(one("replace", "preferredLanguage", "\"en\""))
                .isEqualTo(new SetText(TextAttribute.PREFERRED_LANGUAGE, "en"));
        assertThat(one("replace", "locale", "\"en-GB\""))
                .isEqualTo(new SetText(TextAttribute.LOCALE, "en-GB"));
        assertThat(one("replace", "timezone", "\"Europe/London\""))
                .isEqualTo(new SetText(TextAttribute.TIMEZONE, "Europe/London"));
        assertThat(one("replace", "active", "false")).isEqualTo(new SetActive(false));
        assertThat(one("replace", "password", "\"a-new-password\""))
                .isEqualTo(new SetPassword("a-new-password"));
        assertThat(one("remove", "password", null)).isEqualTo(new RemovePassword());
    }

    /**
     * {@code active} has no unassigned state to remove it to, and reading a remove as the create
     * default would reactivate a deactivated User, so the remove is refused, with or without the
     * schema prefix.
     */
    @ParameterizedTest
    @ValueSource(strings = {"active", "ACTIVE", "urn:ietf:params:scim:schemas:core:2.0:User:active"})
    void removing_active_is_a_mutability_refusal(String path) {
        refused("[{\"op\":\"remove\",\"path\":" + JSON.writeValueAsString(path) + "}]",
                400, "mutability");
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "emails..value", "emails[", "emails[]", "1userName",
            "name.givenName.x", "userName!", "emails[type eq \"work\"]x"})
    void a_malformed_path_or_an_unimplemented_attribute_is_invalid_path(String path) {
        refused("[{\"op\":\"replace\",\"path\":" + JSON.writeValueAsString(path)
                + ",\"value\":\"x\"}]", 400, "invalidPath");
    }

    @Test
    void a_filter_or_sub_attribute_where_the_attribute_has_none_is_invalid_path() {
        refused("[{\"op\":\"replace\",\"path\":\"userName[value eq \\\"x\\\"]\",\"value\":\"x\"}]",
                400, "invalidPath");
        refused("[{\"op\":\"replace\",\"path\":\"userName.first\",\"value\":\"x\"}]",
                400, "invalidPath");
        refused("[{\"op\":\"replace\",\"path\":\"name.nickname\",\"value\":\"x\"}]",
                400, "invalidPath");
        refused("[{\"op\":\"replace\",\"path\":\"emails.label\",\"value\":\"x\"}]",
                400, "invalidPath");
        refused("[{\"op\":\"replace\",\"path\":\"name[givenName eq \\\"x\\\"]\",\"value\":\"x\"}]",
                400, "invalidPath");
        refused("[{\"op\":\"replace\",\"path\":7,\"value\":\"x\"}]", 400, "invalidPath");
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "meta", "groups", "schemas", "Groups"})
    void a_read_only_attribute_as_a_path_is_mutability(String path) {
        refused("[{\"op\":\"replace\",\"path\":\"" + path + "\",\"value\":\"x\"}]",
                400, "mutability");
    }

    /**
     * {@code externalId} is read-write: {@code add} and {@code replace} set it, {@code remove}
     * clears it, by the bare path and by the schema-qualified one, case-insensitively.
     */
    @ParameterizedTest
    @ValueSource(strings = {"externalId", "EXTERNALID",
        "urn:ietf:params:scim:schemas:core:2.0:User:externalId"})
    void external_id_is_set_and_removed_by_its_path(String path) {
        assertThat(read("[{\"op\":\"add\",\"path\":\"" + path + "\",\"value\":\"e1\"}]"))
                .containsExactly(new SetExternalId("e1"));
        assertThat(read("[{\"op\":\"replace\",\"path\":\"" + path + "\",\"value\":\"e2\"}]"))
                .containsExactly(new SetExternalId("e2"));
        assertThat(read("[{\"op\":\"remove\",\"path\":\"" + path + "\"}]"))
                .containsExactly(new RemoveExternalId());
    }

    @Test
    void external_id_takes_a_non_blank_string_and_has_no_sub_attributes() {
        refused("[{\"op\":\"replace\",\"path\":\"externalId\",\"value\":7}]",
                400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"externalId\"}]", 400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"externalId\",\"value\":\" \"}]",
                400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"externalId.value\",\"value\":\"x\"}]",
                400, "invalidPath");
    }

    /** RFC 7644 §3.5.2.2: a remove with no path fails with {@code noTarget}. */
    @Test
    void a_remove_without_a_path_is_no_target() {
        refused("[{\"op\":\"remove\"}]", 400, "noTarget");
    }

    // ---- values ---------------------------------------------------------------------------

    @Test
    void a_value_of_the_wrong_type_or_a_missing_value_is_invalid_value() {
        refused("[{\"op\":\"replace\",\"path\":\"userName\",\"value\":7}]", 400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"userName\"}]", 400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"userName\",\"value\":\" \"}]",
                400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"active\",\"value\":\"false\"}]",
                400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"name\",\"value\":\"Ada\"}]",
                400, "invalidValue");
        refused("[{\"op\":\"add\",\"path\":\"emails\",\"value\":{\"value\":\"a@b.example\"}}]",
                400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"emails[type eq \\\"work\\\"]\","
                + "\"value\":\"a@b.example\"}]", 400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"emails[type eq \\\"work\\\"]\","
                + "\"value\":{\"label\":\"x\"}}]", 400, "invalidValue");
        refused("[{\"op\":\"replace\",\"path\":\"emails[type eq \\\"work\\\"].primary\","
                + "\"value\":\"true\"}]", 400, "invalidValue");
    }

    // ---- name -----------------------------------------------------------------------------

    @Test
    void name_reads_as_a_merge_of_the_named_parts_or_a_removal() {
        assertThat(one("replace", "name", "{\"givenName\":\"Ada\",\"familyName\":\"King\"}"))
                .isEqualTo(new MergeName(new ScimName(null, "King", "Ada", null, null, null)));
        assertThat(one("remove", "name", null)).isEqualTo(new RemoveName());
        assertThat(one("remove", "name.middleName", null))
                .isEqualTo(new RemoveNamePart(NamePart.MIDDLE_NAME));
    }

    @Test
    void each_name_sub_attribute_path_sets_exactly_that_part() {
        assertThat(one("add", "name.formatted", "\"x\""))
                .isEqualTo(new MergeName(new ScimName("x", null, null, null, null, null)));
        assertThat(one("add", "name.familyName", "\"x\""))
                .isEqualTo(new MergeName(new ScimName(null, "x", null, null, null, null)));
        assertThat(one("add", "name.givenName", "\"x\""))
                .isEqualTo(new MergeName(new ScimName(null, null, "x", null, null, null)));
        assertThat(one("add", "name.MIDDLENAME", "\"x\""))
                .isEqualTo(new MergeName(new ScimName(null, null, null, "x", null, null)));
        assertThat(one("add", "name.honorificPrefix", "\"x\""))
                .isEqualTo(new MergeName(new ScimName(null, null, null, null, "x", null)));
        assertThat(one("add", "name.honorificSuffix", "\"x\""))
                .isEqualTo(new MergeName(new ScimName(null, null, null, null, null, "x")));
    }

    // ---- emails ---------------------------------------------------------------------------

    @Test
    void emails_read_as_add_replace_or_remove_of_the_whole_list() {
        ScimEmail work = new ScimEmail("ada@work.example", "work", true);
        String array = "[{\"value\":\"ada@work.example\",\"type\":\"work\",\"primary\":true}]";

        assertThat(one("add", "emails", array)).isEqualTo(new AddEmails(List.of(work)));
        assertThat(one("replace", "emails", array)).isEqualTo(new ReplaceEmails(List.of(work)));
        assertThat(one("remove", "emails", null))
                .isEqualTo(new RemoveEmails(ScimEmailFilter.ALL));
    }

    @Test
    void a_filtered_emails_path_reads_its_filter_and_its_target() {
        assertThat(one("remove", "emails[type eq \"work\"]", null))
                .isEqualTo(new RemoveEmails(typeIs("work")));
        assertThat(one("replace", "emails[type eq \"work\"]",
                "{\"value\":\"a@b.example\",\"primary\":false}"))
                .isEqualTo(new UpdateEmails(typeIs("work"),
                        new EmailUpdate("a@b.example", null, false)));
        assertThat(one("replace", "emails[type eq \"work\"]", "{\"type\":\"home\"}"))
                .isEqualTo(new UpdateEmails(typeIs("work"), new EmailUpdate(null, "home", null)));
        assertThat(one("replace", "emails[type eq \"work\"].value", "\"a@b.example\""))
                .isEqualTo(new UpdateEmails(typeIs("work"),
                        new EmailUpdate("a@b.example", null, null)));
        assertThat(one("add", "emails[type eq \"work\"].type", "\"home\""))
                .isEqualTo(new UpdateEmails(typeIs("work"), new EmailUpdate(null, "home", null)));
        assertThat(one("replace", "emails[type eq \"work\"].primary", "true"))
                .isEqualTo(new UpdateEmails(typeIs("work"), new EmailUpdate(null, null, true)));
        assertThat(one("remove", "emails[type eq \"work\"].type", null))
                .isEqualTo(new RemoveEmailPart(typeIs("work"), ScimEmailPart.TYPE));
        assertThat(one("remove", "emails.primary", null))
                .isEqualTo(new RemoveEmailPart(ScimEmailFilter.ALL, ScimEmailPart.PRIMARY));
    }

    @Test
    void a_filter_joins_eq_comparisons_with_and_outside_quoted_strings() {
        assertThat(one("remove",
                "emails[value eq \"a and b@x.example\" AND primary eq true and type eq null]",
                null))
                .isEqualTo(new RemoveEmails(new ScimEmailFilter(List.of(
                        new Condition(ScimEmailPart.VALUE, "a and b@x.example"),
                        new Condition(ScimEmailPart.PRIMARY, true),
                        new Condition(ScimEmailPart.TYPE, null)))));
        assertThat(one("remove", "emails[value eq \"quote\\\" and x\"]", null))
                .isEqualTo(new RemoveEmails(new ScimEmailFilter(List.of(
                        new Condition(ScimEmailPart.VALUE, "quote\" and x")))));
    }

    /** Refused rather than approximated: an approximation selects values nobody asked for. */
    @ParameterizedTest
    @ValueSource(strings = {
            "emails[type co \"wo\"]",
            "emails[type ne \"work\"]",
            "emails[type pr]",
            "emails[type eq \"work\" or type eq \"home\"]",
            "emails[not (type eq \"work\")]",
            "emails[(type eq \"work\")]",
            "emails[label eq \"x\"]",
            "emails[primary eq \"true\"]",
            "emails[type eq 7]",
            "emails[type eq work]"})
    void any_other_filter_is_invalid_filter(String path) {
        refused("[{\"op\":\"remove\",\"path\":" + JSON.writeValueAsString(path) + "}]",
                400, "invalidFilter");
    }

    @Test
    void an_unterminated_string_in_a_filter_is_invalid_path() {
        refused("[{\"op\":\"remove\",\"path\":\"emails[type eq \\\"work]\"}]", 400, "invalidPath");
    }

    /** An escape as the very last character of the filter has nothing to escape; not a 500. */
    @Test
    void a_trailing_escape_inside_a_string_is_an_unterminated_string_not_a_crash() {
        refused("[{\"op\":\"remove\",\"path\":\"emails[value eq \\\"abc\\\\]\"}]",
                400, "invalidPath");
    }

    /** A conjunction with nothing after it ends the text; still a refusal, not a 500. */
    @Test
    void a_trailing_and_with_no_comparison_after_it_is_invalid_filter() {
        refused("[{\"op\":\"remove\",\"path\":\"emails[type eq \\\"work\\\" and]\"}]",
                400, "invalidFilter");
    }

    // ---- no path --------------------------------------------------------------------------

    /** Each attribute of the value applies as if it had been the path; read-only ones are ignored. */
    @Test
    void a_pathless_operation_expands_into_one_operation_per_attribute() {
        assertThat(read("""
                [{"op":"replace","value":{"displayName":"Ada","active":false,
                  "id":"ignored","meta":{},"groups":[],"schemas":[],"externalId":"x"}}]"""))
                .containsExactly(
                        new SetText(TextAttribute.DISPLAY_NAME, "Ada"),
                        new SetActive(false),
                        new SetExternalId("x"));
    }

    @Test
    void a_pathless_operation_needs_an_object_of_plain_attribute_names() {
        refused("[{\"op\":\"replace\",\"value\":\"Ada\"}]", 400, "invalidValue");
        refused("[{\"op\":\"replace\"}]", 400, "invalidValue");
        refused("[{\"op\":\"replace\",\"value\":{\"name.givenName\":\"Ada\"}}]",
                400, "invalidPath");
        refused("[{\"op\":\"replace\",\"value\":{\"emails[type eq \\\"w\\\"]\":{}}}]",
                400, "invalidPath");
        refused("[{\"op\":\"replace\",\"value\":{\"title\":\"x\"}}]", 400, "invalidPath");
    }

    @Test
    void operations_are_read_in_order() {
        assertThat(read("""
                [{"op":"replace","path":"active","value":false},
                 {"op":"remove","path":"displayName"},
                 {"op":"add","path":"locale","value":"en-GB"}]"""))
                .containsExactly(
                        new SetActive(false),
                        new RemoveText(TextAttribute.DISPLAY_NAME),
                        new SetText(TextAttribute.LOCALE, "en-GB"));
    }

    /** The per-request bound: 100 operations are read, the 101st makes the body a refusal. */
    @Test
    void a_patch_op_carries_at_most_one_hundred_operations() {
        String operation = "{\"op\":\"replace\",\"path\":\"active\",\"value\":false}";
        assertThat(read("[" + String.join(",", Collections.nCopies(100, operation)) + "]"))
                .hasSize(100);
        refused("[" + String.join(",", Collections.nCopies(101, operation)) + "]",
                400, "invalidValue");
    }
}
