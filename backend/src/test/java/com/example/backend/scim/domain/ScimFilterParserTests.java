package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.scim.domain.ScimFilter.And;
import com.example.backend.scim.domain.ScimFilter.AttributeRef;
import com.example.backend.scim.domain.ScimFilter.Comparison;
import com.example.backend.scim.domain.ScimFilter.Not;
import com.example.backend.scim.domain.ScimFilter.Operator;
import com.example.backend.scim.domain.ScimFilter.Or;
import com.example.backend.scim.domain.ScimFilter.Presence;
import com.example.backend.scim.domain.ScimFilter.ValuePath;
import java.time.Instant;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The grammar, the validation and the limits, without a database.
 *
 * <p>Asserts the TREE, not just "it parsed": precedence, case folding, schema qualification and
 * the complex-attribute rewrite are all claims about which node goes where, and a parser that
 * accepted everything and built the wrong tree would pass a test that only checked for no
 * exception.
 */
class ScimFilterParserTests {

    private static final Set<ScimResourceType> USERS = Set.of(ScimResourceType.USER);

    private static final Set<ScimResourceType> GROUPS = Set.of(ScimResourceType.GROUP);

    private static final Set<ScimResourceType> BOTH =
            Set.of(ScimResourceType.USER, ScimResourceType.GROUP);

    private static ScimFilter users(String filter) {
        return ScimFilterParser.parse(filter, USERS);
    }

    private static AttributeRef ref(ScimFilterPath path) {
        return new AttributeRef(path, null);
    }

    private static Comparison eq(ScimFilterPath path, Object value) {
        return new Comparison(ref(path), Operator.EQ, value);
    }

    // ---- grammar -------------------------------------------------------------------------------

    @Test
    void a_comparison_is_its_path_operator_and_literal() {
        assertThat(users("userName eq \"bjensen\"")).isEqualTo(eq(ScimFilterPath.USER_NAME, "bjensen"));
    }

    @Test
    void names_operators_and_keywords_are_case_insensitive() {
        assertThat(users("USERNAME Eq \"bjensen\" AND ACTIVE eQ TRUE"))
                .isEqualTo(new And(
                        eq(ScimFilterPath.USER_NAME, "bjensen"),
                        eq(ScimFilterPath.ACTIVE, true)));
    }

    @Test
    void every_comparison_operator_is_recognised() {
        for (Operator operator : Operator.values()) {
            assertThat(users("userName " + operator.token() + " \"a\""))
                    .isEqualTo(new Comparison(ref(ScimFilterPath.USER_NAME), operator, "a"));
        }
    }

    /** {@code and} binds tighter than {@code or}: a or (b and c), never (a or b) and c. */
    @Test
    void and_binds_tighter_than_or() {
        assertThat(users("userName eq \"a\" or userName eq \"b\" and active eq true"))
                .isEqualTo(new Or(
                        eq(ScimFilterPath.USER_NAME, "a"),
                        new And(eq(ScimFilterPath.USER_NAME, "b"), eq(ScimFilterPath.ACTIVE, true))));
        assertThat(users("userName eq \"a\" and userName eq \"b\" or active eq true"))
                .isEqualTo(new Or(
                        new And(eq(ScimFilterPath.USER_NAME, "a"), eq(ScimFilterPath.USER_NAME, "b")),
                        eq(ScimFilterPath.ACTIVE, true)));
    }

    @Test
    void parentheses_override_precedence_and_chains_associate_left() {
        assertThat(users("(userName eq \"a\" or userName eq \"b\") and active eq true"))
                .isEqualTo(new And(
                        new Or(eq(ScimFilterPath.USER_NAME, "a"), eq(ScimFilterPath.USER_NAME, "b")),
                        eq(ScimFilterPath.ACTIVE, true)));
        assertThat(users("userName pr or locale pr or timezone pr"))
                .isEqualTo(new Or(
                        new Or(new Presence(ref(ScimFilterPath.USER_NAME)),
                                new Presence(ref(ScimFilterPath.LOCALE))),
                        new Presence(ref(ScimFilterPath.TIMEZONE))));
    }

    @Test
    void not_negates_its_parenthesised_filter_and_binds_tightest() {
        assertThat(users("not (userName eq \"a\") and active eq true"))
                .isEqualTo(new And(
                        new Not(eq(ScimFilterPath.USER_NAME, "a")),
                        eq(ScimFilterPath.ACTIVE, true)));
    }

    @Test
    void whitespace_around_tokens_is_insignificant() {
        assertThat(users("  (  userName   eq\"a\"  )  "))
                .isEqualTo(eq(ScimFilterPath.USER_NAME, "a"));
    }

    @Test
    void a_schema_qualified_path_records_the_schema_it_named() {
        assertThat(users("urn:ietf:params:scim:schemas:core:2.0:User:name.familyName eq \"J\""))
                .isEqualTo(new Comparison(
                        new AttributeRef(ScimFilterPath.NAME_FAMILY_NAME, ScimResourceType.USER),
                        Operator.EQ,
                        "J"));
        assertThat(users("URN:IETF:PARAMS:SCIM:SCHEMAS:CORE:2.0:USER:userName pr"))
                .isEqualTo(new Presence(
                        new AttributeRef(ScimFilterPath.USER_NAME, ScimResourceType.USER)));
    }

    @Test
    void a_value_path_holds_a_filter_over_the_attributes_sub_attributes() {
        assertThat(users("emails[type eq \"work\" and not (primary eq true)]"))
                .isEqualTo(new ValuePath(
                        ref(ScimFilterPath.EMAILS),
                        new And(
                                eq(ScimFilterPath.EMAILS_TYPE, "work"),
                                new Not(eq(ScimFilterPath.EMAILS_PRIMARY, true)))));
    }

    /** RFC 7644's own example: a bare multi-valued complex attribute compares its value. */
    @Test
    void a_bare_multi_valued_attribute_is_compared_through_its_value() {
        assertThat(users("emails co \"example.com\""))
                .isEqualTo(new Comparison(ref(ScimFilterPath.EMAILS_VALUE), Operator.CO, "example.com"));
        assertThat(ScimFilterParser.parse("members eq \"x\"", GROUPS))
                .isEqualTo(eq(ScimFilterPath.MEMBERS_VALUE, "x"));
    }

    @Test
    void presence_applies_to_simple_and_complex_attributes() {
        assertThat(users("name pr")).isEqualTo(new Presence(ref(ScimFilterPath.NAME)));
        assertThat(users("emails pr")).isEqualTo(new Presence(ref(ScimFilterPath.EMAILS)));
        assertThat(users("meta pr")).isEqualTo(new Presence(ref(ScimFilterPath.META)));
    }

    @Test
    void literals_are_typed() {
        assertThat(users("displayName eq null")).isEqualTo(eq(ScimFilterPath.DISPLAY_NAME, null));
        assertThat(users("displayName ne NULL"))
                .isEqualTo(new Comparison(ref(ScimFilterPath.DISPLAY_NAME), Operator.NE, null));
        assertThat(users("active eq False")).isEqualTo(eq(ScimFilterPath.ACTIVE, false));
        assertThat(users("meta.created ge \"2011-05-13T04:42:34+08:00\""))
                .isEqualTo(new Comparison(
                        ref(ScimFilterPath.META_CREATED),
                        Operator.GE,
                        Instant.parse("2011-05-12T20:42:34Z")));
    }

    /** JSON string escapes decode to the characters they denote, and nothing else is special. */
    @Test
    void a_string_literal_decodes_its_json_escapes() {
        assertThat(users("userName eq \"a\\\"b\\\\c\\/d\\n\\t\\r\\b\\f\\u00e9\""))
                .isEqualTo(eq(ScimFilterPath.USER_NAME, "a\"b\\c/d\n\t\r\b\fé"));
        assertThat(users("userName eq \"x' OR '1'='1 -- ; DROP TABLE t; %_ :p0 ?\""))
                .isEqualTo(eq(ScimFilterPath.USER_NAME, "x' OR '1'='1 -- ; DROP TABLE t; %_ :p0 ?"));
    }

    // ---- vocabulary ----------------------------------------------------------------------------

    @Test
    void the_base_search_accepts_a_path_either_type_has() {
        assertThat(ScimFilterParser.parse("userName pr", BOTH))
                .isEqualTo(new Presence(ref(ScimFilterPath.USER_NAME)));
        assertThat(ScimFilterParser.parse("members pr", BOTH))
                .isEqualTo(new Presence(ref(ScimFilterPath.MEMBERS)));
        assertThat(ScimFilterParser.parse(
                        "urn:ietf:params:scim:schemas:core:2.0:Group:displayName pr", BOTH))
                .isEqualTo(new Presence(
                        new AttributeRef(ScimFilterPath.DISPLAY_NAME, ScimResourceType.GROUP)));
    }

    @Test
    void an_attribute_reference_applies_to_its_schemas_type_only() {
        AttributeRef group = new AttributeRef(ScimFilterPath.DISPLAY_NAME, ScimResourceType.GROUP);
        AttributeRef any = ref(ScimFilterPath.DISPLAY_NAME);

        assertThat(group.appliesTo(ScimResourceType.GROUP)).isTrue();
        assertThat(group.appliesTo(ScimResourceType.USER)).isFalse();
        assertThat(any.appliesTo(ScimResourceType.USER)).isTrue();
        assertThat(any.appliesTo(ScimResourceType.GROUP)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "password eq \"secret\"",
            "PASSWORD pr",
            "urn:ietf:params:scim:schemas:core:2.0:User:password ne \"x\""})
    void the_password_is_never_filterable(String filter) {
        assertThatThrownBy(() -> ScimFilterParser.parse(filter, BOTH))
                .isInstanceOf(InvalidScimFilterException.class)
                .hasMessageContaining("password");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "members pr",
            "urn:ietf:params:scim:schemas:core:2.0:Group:displayName eq \"x\"",
            "nickName eq \"x\"",
            "name.nickName eq \"x\"",
            "urn:example:custom:1.0:User:userName eq \"x\"",
            "emails[display eq \"x\"]",
            "emails[emails.value eq \"x\"]"})
    void a_path_the_queried_types_lack_is_refused(String filter) {
        assertThatThrownBy(() -> users(filter)).isInstanceOf(InvalidScimFilterException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "active gt true", "active co \"t\"", "active eq \"true\"", "active eq 1",
            "userName eq true", "userName eq 5", "userName eq -1.5e3",
            "meta.created co \"2020\"", "meta.created eq \"not a date\"", "meta.created eq true",
            "userName lt null", "userName co null",
            "name eq \"x\"", "meta eq \"x\"", "userName[value eq \"x\"]", "name[givenName eq \"x\"]"})
    void an_operator_or_literal_the_attribute_type_does_not_support_is_refused(String filter) {
        assertThatThrownBy(() -> users(filter)).isInstanceOf(InvalidScimFilterException.class);
    }

    // ---- malformed -----------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "userName", "userName eq", "userName eq bjensen", "userName xx \"a\"",
            "(userName pr", "userName pr)", "userName pr and", "and userName pr",
            "not userName pr", "not (userName pr", "userName eq \"open", "userName eq \"bad \\q\"",
            "userName eq \"bad \\u12\"", "userName eq \"bad \\uzzzz\"", "userName eq \"nul \\u0000\"",
            "userName eq \"tab\tinside\"", "userName eq \"a\" \"b\"", "emails[type eq \"w\"",
            "emails[type eq \"w\"]]", "emails[type eq \"w\"][value pr]", "emails[value[type pr]]",
            "userName eq \"a\" userName eq \"b\"", "()", "[userName pr]", "userName pr pr"})
    void a_malformed_filter_is_refused(String filter) {
        assertThatThrownBy(() -> users(filter)).isInstanceOf(InvalidScimFilterException.class);
    }

    /**
     * A refusal says WHICH rule refused the filter: the detail is what a connector's operator
     * reads to fix it, and several refusals would otherwise fall through to a later, vaguer one
     * (an empty filter to "attribute path expected", the password to "not filterable", a
     * foreign schema to "not an attribute of the queried types").
     */
    static Stream<Arguments> refusals() {
        return Stream.of(
                Arguments.of("   ", "The filter is empty."),
                Arguments.of("()", "An attribute path was expected."),
                Arguments.of("password pr", "password cannot be filtered on."),
                Arguments.of("emails[type[value eq \"x\"]]",
                        "A value path cannot be nested in another."),
                Arguments.of("name[givenName eq \"x\"]",
                        "Only a multi-valued complex attribute takes a value filter: name"),
                Arguments.of("emails.type[value eq \"x\"]",
                        "Only a multi-valued complex attribute takes a value filter: emails.type"),
                Arguments.of("emails[nope eq \"x\"]", "Not a sub-attribute of emails."),
                Arguments.of("urn:ietf:params:scim:schemas:core:2.0:Group:displayName eq \"x\"",
                        "The filter names a schema that is not queried here."),
                Arguments.of("members pr",
                        "Not a filterable attribute of the queried resource types: members"),
                Arguments.of(".userName pr",
                        "The filter names an attribute this service does not support."),
                Arguments.of("name eq \"x\"",
                        "A complex attribute is compared through a sub-attribute: name"),
                Arguments.of("active gt true", "A boolean is compared only with eq or ne: active"),
                Arguments.of("meta.created co \"2020-01-01T00:00:00Z\"",
                        "A dateTime does not support substring operators: meta.created"),
                Arguments.of("userName eq 5", "The compared value does not match the type of userName."),
                Arguments.of("active eq \"true\"", "The compared value does not match the type of active."),
                Arguments.of("userName eq bjensen", "A value was expected after the operator."),
                Arguments.of("userName eq \"abc\\", "A string value is not terminated."),
                Arguments.of("userName eq \"\\u0041", "A string value is not terminated."),
                Arguments.of("userName eq \"\\u004", "A string value has an invalid escape."),
                Arguments.of("userName eq \"a\u001fb\"", "A string value contains a control character."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("refusals")
    void a_refusal_names_the_rule_that_refused_it(String filter, String detail) {
        assertThatThrownBy(() -> users(filter))
                .isInstanceOf(InvalidScimFilterException.class)
                .hasMessage(detail);
    }

    /** The lexer's edges: a space is ordinary text, and whitespace before a bracket is skipped. */
    @Test
    void the_lexer_accepts_what_sits_just_inside_its_rules() {
        assertThat(users("userName eq \"a b\u0020c\"")).isEqualTo(eq(ScimFilterPath.USER_NAME, "a b c"));
        assertThat(users("userName pr   ")).isEqualTo(new Presence(ref(ScimFilterPath.USER_NAME)));
        assertThat(users("emails [type eq \"work\"]"))
                .isEqualTo(new ValuePath(ref(ScimFilterPath.EMAILS), eq(ScimFilterPath.EMAILS_TYPE, "work")));
        // a bare literal ends at a closing bracket or parenthesis
        assertThat(users("emails[primary eq true]"))
                .isEqualTo(new ValuePath(ref(ScimFilterPath.EMAILS), eq(ScimFilterPath.EMAILS_PRIMARY, true)));
        assertThat(users("(active eq false)")).isEqualTo(eq(ScimFilterPath.ACTIVE, false));
        // a keyword ends at an opening parenthesis
        assertThat(users("not(active eq false)")).isEqualTo(new Not(eq(ScimFilterPath.ACTIVE, false)));
        assertThat(users("userName pr and(active eq false)"))
                .isEqualTo(new And(new Presence(ref(ScimFilterPath.USER_NAME)), eq(ScimFilterPath.ACTIVE, false)));
    }

    /** A schema-qualified bare multi-valued attribute keeps its schema on the value it compares. */
    @Test
    void a_qualified_bare_multi_valued_attribute_keeps_its_schema() {
        assertThat(ScimFilterParser.parse(
                        "urn:ietf:params:scim:schemas:core:2.0:User:emails co \"x\"", BOTH))
                .isEqualTo(new Comparison(
                        new AttributeRef(ScimFilterPath.EMAILS_VALUE, ScimResourceType.USER),
                        Operator.CO,
                        "x"));
    }

    @Test
    void a_null_filter_text_is_refused() {
        assertThatThrownBy(() -> ScimFilterParser.parse(null, USERS))
                .isInstanceOf(InvalidScimFilterException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "userName eq \"a-very-secret-value\" and",
            "password eq \"a-very-secret-value\"",
            "meta.created gt \"a-very-secret-value\"",
            "userName eq \"a-very-secret-value\" xx"})
    void a_refusal_never_quotes_a_literal(String filter) {
        assertThatThrownBy(() -> users(filter))
                .isInstanceOf(InvalidScimFilterException.class)
                .satisfies(refusal -> assertThat(refusal.getMessage())
                        .doesNotContain("a-very-secret-value"));
    }

    // ---- limits --------------------------------------------------------------------------------

    @Test
    void text_up_to_the_byte_limit_is_accepted_and_one_byte_more_is_refused() {
        String prefix = "userName eq \"";
        int room = ScimFilterParser.MAX_FILTER_BYTES - prefix.length() - 1;
        String atLimit = prefix + "a".repeat(room) + "\"";
        assertThat(atLimit.length()).isEqualTo(ScimFilterParser.MAX_FILTER_BYTES);

        assertThat(users(atLimit)).isInstanceOf(Comparison.class);
        assertThatThrownBy(() -> users(prefix + "a".repeat(room + 1) + "\""))
                .isInstanceOf(InvalidScimFilterException.class)
                .hasMessageContaining("bytes");
    }

    /** Bytes, not characters: a multi-byte character spends more of the limit. */
    @Test
    void the_text_limit_counts_utf8_bytes() {
        String prefix = "userName eq \"";
        int room = ScimFilterParser.MAX_FILTER_BYTES - prefix.length() - 1;
        String twoByte = prefix + "é".repeat(room / 2 + 1) + "\"";
        assertThat(twoByte.length()).isLessThan(ScimFilterParser.MAX_FILTER_BYTES);

        assertThatThrownBy(() -> users(twoByte)).isInstanceOf(InvalidScimFilterException.class);
    }

    @Test
    void nesting_up_to_the_depth_limit_is_accepted_and_one_level_more_is_refused() {
        // The comparison itself is one level; each enclosing parenthesis is one more.
        int parentheses = ScimFilterParser.MAX_DEPTH - 1;
        String atLimit = "(".repeat(parentheses) + "userName pr" + ")".repeat(parentheses);
        String over = "(".repeat(parentheses + 1) + "userName pr" + ")".repeat(parentheses + 1);

        assertThat(users(atLimit)).isEqualTo(new Presence(ref(ScimFilterPath.USER_NAME)));
        assertThatThrownBy(() -> users(over))
                .isInstanceOf(InvalidScimFilterException.class)
                .hasMessageContaining("deeper");
    }

    @Test
    void not_and_value_paths_count_toward_the_depth_limit() {
        String nots = "not (".repeat(ScimFilterParser.MAX_DEPTH) + "userName pr"
                + ")".repeat(ScimFilterParser.MAX_DEPTH);
        assertThatThrownBy(() -> users(nots)).isInstanceOf(InvalidScimFilterException.class);

        // Inside a value path the inner comparison is two levels below the attribute: the value
        // path itself, then the comparison. So k parentheses put it at depth k + 3.
        String nestedInValuePath = "(".repeat(ScimFilterParser.MAX_DEPTH - 3) + "emails[type pr]"
                + ")".repeat(ScimFilterParser.MAX_DEPTH - 3);
        String overInValuePath = "(".repeat(ScimFilterParser.MAX_DEPTH - 2) + "emails[type pr]"
                + ")".repeat(ScimFilterParser.MAX_DEPTH - 2);
        assertThat(users(nestedInValuePath)).isInstanceOf(ValuePath.class);
        assertThatThrownBy(() -> users(overInValuePath)).isInstanceOf(InvalidScimFilterException.class);
    }

    /** A depth far past the limit is refused, not a stack overflow. */
    @Test
    void a_pathologically_deep_filter_is_refused_before_it_can_exhaust_the_stack() {
        String deep = "(".repeat(4000) + "userName pr" + ")".repeat(4000);

        assertThatThrownBy(() -> users(deep)).isInstanceOf(InvalidScimFilterException.class);
    }

    @Test
    void expression_nodes_up_to_the_limit_are_accepted_and_one_more_is_refused() {
        // n comparisons joined by n-1 `or`s is 2n-1 nodes; one `not` makes the count even.
        String fiftyComparisons = orChain(50);
        String hundredNodes = "not (" + fiftyComparisons + ")";
        String hundredAndOne = orChain(51);

        assertThat(users(hundredNodes)).isInstanceOf(Not.class);
        assertThatThrownBy(() -> users(hundredAndOne))
                .isInstanceOf(InvalidScimFilterException.class)
                .hasMessageContaining("expressions");
    }

    @Test
    void value_path_nodes_count_toward_the_node_limit() {
        // Each `emails[type pr]` is two nodes; 34 of them and 33 `or`s is 101.
        StringBuilder filter = new StringBuilder("emails[type pr]");
        for (int i = 1; i < 34; i++) {
            filter.append(" or emails[type pr]");
        }
        assertThatThrownBy(() -> users(filter.toString())).isInstanceOf(InvalidScimFilterException.class);
    }

    private static String orChain(int comparisons) {
        StringBuilder chain = new StringBuilder("userName eq \"a0\"");
        for (int i = 1; i < comparisons; i++) {
            chain.append(" or userName eq \"a").append(i).append('"');
        }
        return chain.toString();
    }

    // ---- sortBy paths --------------------------------------------------------------------------

    @Test
    void a_single_path_resolves_to_its_reference_and_attribute() {
        ScimFilterParser.ResolvedPath path = ScimFilterParser.parsePath(" emails.VALUE ", USERS);

        assertThat(path.reference()).isEqualTo(ref(ScimFilterPath.EMAILS_VALUE));
        assertThat(path.attribute().multiValued()).isTrue();
        assertThat(path.attribute().kind()).isEqualTo(ScimQueryVocabulary.Kind.STRING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "password", "nickName", "members.value", "userName eq"})
    void a_single_path_the_type_lacks_is_refused(String path) {
        assertThatThrownBy(() -> ScimFilterParser.parsePath(path, USERS))
                .isInstanceOf(InvalidScimFilterException.class);
    }

    /**
     * A schema-qualified path resolves to THAT type's attribute even when the other queried type
     * has one of the same name: a Group's displayName sorts case-insensitively, a User's does not.
     */
    @Test
    void a_qualified_single_path_resolves_against_its_own_schemas_vocabulary() {
        ScimFilterParser.ResolvedPath group = ScimFilterParser.parsePath(
                "urn:ietf:params:scim:schemas:core:2.0:Group:displayName", BOTH);
        ScimFilterParser.ResolvedPath user = ScimFilterParser.parsePath(
                "urn:ietf:params:scim:schemas:core:2.0:User:displayName", BOTH);

        assertThat(group.attribute().caseExact()).isFalse();
        assertThat(user.attribute().caseExact()).isTrue();
    }

    @Test
    void an_empty_single_path_is_refused_as_empty() {
        assertThatThrownBy(() -> ScimFilterParser.parsePath("  ", USERS))
                .isInstanceOf(InvalidScimFilterException.class)
                .hasMessage("The attribute path is empty.");
    }

    @Test
    void a_null_single_path_is_refused() {
        assertThatThrownBy(() -> ScimFilterParser.parsePath(null, USERS))
                .isInstanceOf(InvalidScimFilterException.class);
    }

    // ---- fuzz ----------------------------------------------------------------------------------

    /**
     * Property: for any input at all the parser either returns a tree or refuses with
     * {@link InvalidScimFilterException} — never another exception, never a hang. Inputs are
     * built from the grammar's own tokens and metacharacters so a good share are near-misses
     * that reach deep into the parser. Seeded, so a failure reproduces.
     */
    @Test
    void any_input_is_either_a_filter_or_an_invalid_filter() {
        String[] alphabet = {
                "userName", "emails", "name.familyName", "meta.created", "active", "members",
                "urn:ietf:params:scim:schemas:core:2.0:User:", "password", " eq ", " ne ", " co ",
                " sw ", " gt ", " pr", " and ", " or ", "not ", "(", ")", "[", "]", "\"", "\\",
                "\\u", "0", "true", "null", "'", ";", "--", "%", "_", "é", " ", ".", "type", "$ref",
                "\"2020-01-01T00:00:00Z\"", "\"x\""};
        Random random = new Random(20260929L);
        int parsed = 0;
        for (int round = 0; round < 20_000; round++) {
            StringBuilder input = new StringBuilder();
            int tokens = 1 + random.nextInt(14);
            for (int t = 0; t < tokens; t++) {
                input.append(alphabet[random.nextInt(alphabet.length)]);
            }
            try {
                assertThat(ScimFilterParser.parse(input.toString(), BOTH)).isNotNull();
                parsed++;
            } catch (InvalidScimFilterException refused) {
                assertThat(refused.getMessage()).isNotBlank();
            }
        }
        assertThat(parsed).as("the fuzz reaches the accepting paths too").isPositive();
    }
}
