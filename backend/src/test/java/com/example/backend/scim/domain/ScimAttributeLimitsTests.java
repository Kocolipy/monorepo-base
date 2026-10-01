package com.example.backend.scim.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every stored User and Group attribute is held to its column's width, counted the way the
 * column counts, and a refusal names the attribute and the limit but never carries the value.
 */
class ScimAttributeLimitsTests {

    /** A character outside the Basic Multilingual Plane: one code point, two UTF-16 units. */
    private static final String SUPPLEMENTARY = "\uD83D\uDE00";

    /** U+FDFA, a single code point NFKC expands into eighteen. */
    private static final String EXPANDS_UNDER_NFKC = "\uFDFA";

    private static final ScimUserProfile WITHIN = new ScimUserProfile(
            "ada",
            new ScimName("Ada King", "King", "Ada", "Augusta", "Hon.", "Countess"),
            "Ada",
            "en",
            "en-GB",
            "Europe/London",
            true,
            List.of(new ScimEmail("ada@work.example", "work", true)));

    /** One per stored User attribute: the path a refusal names, its limit, and a profile with it. */
    static Stream<Arguments> userAttributes() {
        return Stream.of(
                user("userName", 256, v -> profile(v, ScimName.NONE, null, null, null, null,
                        List.of())),
                user("name.formatted", 256, v -> named(new ScimName(v, null, null, null, null,
                        null))),
                user("name.familyName", 256, v -> named(new ScimName(null, v, null, null, null,
                        null))),
                user("name.givenName", 256, v -> named(new ScimName(null, null, v, null, null,
                        null))),
                user("name.middleName", 256, v -> named(new ScimName(null, null, null, v, null,
                        null))),
                user("name.honorificPrefix", 256, v -> named(new ScimName(null, null, null, null,
                        v, null))),
                user("name.honorificSuffix", 256, v -> named(new ScimName(null, null, null, null,
                        null, v))),
                user("displayName", 256, v -> profile("ada", ScimName.NONE, v, null, null, null,
                        List.of())),
                user("preferredLanguage", 64, v -> profile("ada", ScimName.NONE, null, v, null,
                        null, List.of())),
                user("locale", 64, v -> profile("ada", ScimName.NONE, null, null, v, null,
                        List.of())),
                user("timezone", 64, v -> profile("ada", ScimName.NONE, null, null, null, v,
                        List.of())),
                user("emails.value", 256, v -> profile("ada", ScimName.NONE, null, null, null,
                        null, List.of(new ScimEmail(v, "work", false)))),
                user("emails.type", 32, v -> profile("ada", ScimName.NONE, null, null, null, null,
                        List.of(new ScimEmail("ada@work.example", v, false)))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("userAttributes")
    void a_user_value_at_its_limit_is_accepted_and_one_more_is_refused_naming_it(
            String attribute, int limit, Function<String, ScimUserProfile> withValue) {
        assertThatCode(() -> ScimAttributeLimits.requireWithin(withValue.apply("a".repeat(limit))))
                .doesNotThrowAnyException();

        String tooLong = "b".repeat(limit + 1);
        assertThatThrownBy(() -> ScimAttributeLimits.requireWithin(withValue.apply(tooLong)))
                .isInstanceOfSatisfying(ScimValueTooLongException.class, refused -> {
                    assertThat(refused.attribute()).isEqualTo(attribute);
                    assertThat(refused.limit()).isEqualTo(limit);
                    assertThat(refused.getMessage()).doesNotContain(tooLong);
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("userAttributes")
    void a_user_value_is_counted_in_code_points_as_the_column_counts_it(
            String attribute, int limit, Function<String, ScimUserProfile> withValue) {
        // Twice `limit` UTF-16 units, exactly `limit` characters: the column stores it.
        assertThatCode(() -> ScimAttributeLimits.requireWithin(
                withValue.apply(SUPPLEMENTARY.repeat(limit))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ScimAttributeLimits.requireWithin(
                withValue.apply(SUPPLEMENTARY.repeat(limit + 1))))
                .isInstanceOf(ScimValueTooLongException.class);
    }

    @Test
    void a_profile_within_every_limit_is_accepted() {
        assertThatCode(() -> ScimAttributeLimits.requireWithin(WITHIN)).doesNotThrowAnyException();
    }

    @Test
    void every_email_is_checked_not_only_the_first() {
        ScimUserProfile secondTooLong = profile("ada", ScimName.NONE, null, null, null, null,
                List.of(new ScimEmail("ada@work.example", "work", true),
                        new ScimEmail("ada@home.example", "t".repeat(33), false)));

        assertThatThrownBy(() -> ScimAttributeLimits.requireWithin(secondTooLong))
                .isInstanceOfSatisfying(ScimValueTooLongException.class,
                        refused -> assertThat(refused.attribute()).isEqualTo("emails.type"));
    }

    /**
     * The normalized form is a column of the same width, and NFKC can lengthen a name: fifteen
     * copies of U+FDFA are fifteen characters submitted and 270 normalized, fourteen are 252.
     */
    @Test
    void a_user_name_that_fits_but_normalizes_past_the_limit_is_refused() {
        String fits = EXPANDS_UNDER_NFKC.repeat(15);
        assertThat(fits.codePointCount(0, fits.length())).isEqualTo(15);
        assertThat(NormalizedUserName.of(fits).value().length()).isGreaterThan(256);

        assertThatThrownBy(() -> ScimAttributeLimits.requireWithin(
                profile(fits, ScimName.NONE, null, null, null, null, List.of())))
                .isInstanceOfSatisfying(ScimValueTooLongException.class, refused -> {
                    assertThat(refused.attribute()).isEqualTo("userName");
                    assertThat(refused.limit()).isEqualTo(256);
                });
        assertThatCode(() -> ScimAttributeLimits.requireWithin(profile(
                EXPANDS_UNDER_NFKC.repeat(14), ScimName.NONE, null, null, null, null, List.of())))
                .doesNotThrowAnyException();
    }

    /**
     * The converse: the submitted form is stored too, so a name that NFKC would shorten to fit is
     * still refused when it is too long as sent. {@code e} plus a combining acute is two code
     * points that compose into one.
     */
    @Test
    void a_name_too_long_as_submitted_is_refused_even_though_it_normalizes_to_fit() {
        String shrinks = "e\u0301".repeat(200);
        assertThat(shrinks.codePointCount(0, shrinks.length())).isEqualTo(400);
        assertThat(NormalizedUserName.of(shrinks).value().length()).isEqualTo(200);

        assertThatThrownBy(() -> ScimAttributeLimits.requireWithin(
                profile(shrinks, ScimName.NONE, null, null, null, null, List.of())))
                .isInstanceOfSatisfying(ScimValueTooLongException.class,
                        refused -> assertThat(refused.attribute()).isEqualTo("userName"));
        assertThatThrownBy(() -> ScimAttributeLimits.requireGroupDisplayNameWithin(shrinks))
                .isInstanceOfSatisfying(ScimValueTooLongException.class,
                        refused -> assertThat(refused.attribute()).isEqualTo("displayName"));
    }

    @Test
    void a_group_display_name_is_held_to_256_submitted_and_normalized() {
        assertThatCode(() -> ScimAttributeLimits.requireGroupDisplayNameWithin("g".repeat(256)))
                .doesNotThrowAnyException();
        assertThatCode(() -> ScimAttributeLimits.requireGroupDisplayNameWithin(
                SUPPLEMENTARY.repeat(256)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ScimAttributeLimits.requireGroupDisplayNameWithin(
                "g".repeat(257)))
                .isInstanceOfSatisfying(ScimValueTooLongException.class, refused -> {
                    assertThat(refused.attribute()).isEqualTo("displayName");
                    assertThat(refused.limit()).isEqualTo(256);
                });
        assertThatThrownBy(() -> ScimAttributeLimits.requireGroupDisplayNameWithin(
                EXPANDS_UNDER_NFKC.repeat(15)))
                .isInstanceOfSatisfying(ScimValueTooLongException.class,
                        refused -> assertThat(refused.attribute()).isEqualTo("displayName"));
    }

    @Test
    void an_external_id_is_held_to_256_and_an_absent_one_is_within() {
        assertThatCode(() -> ScimAttributeLimits.requireExternalIdWithin(null))
                .doesNotThrowAnyException();
        assertThatCode(() -> ScimAttributeLimits.requireExternalIdWithin("x".repeat(256)))
                .doesNotThrowAnyException();
        assertThatCode(() -> ScimAttributeLimits.requireExternalIdWithin(
                SUPPLEMENTARY.repeat(256)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ScimAttributeLimits.requireExternalIdWithin("x".repeat(257)))
                .isInstanceOfSatisfying(ScimValueTooLongException.class, refused -> {
                    assertThat(refused.attribute()).isEqualTo("externalId");
                    assertThat(refused.limit()).isEqualTo(256);
                    assertThat(refused.getMessage())
                            .isEqualTo("externalId is longer than 256 characters.");
                });
    }

    private static Arguments user(
            String attribute, int limit, Function<String, ScimUserProfile> withValue) {
        return Arguments.of(attribute, limit, withValue);
    }

    private static ScimUserProfile named(ScimName name) {
        return profile("ada", name, null, null, null, null, List.of());
    }

    private static ScimUserProfile profile(
            String userName,
            ScimName name,
            String displayName,
            String preferredLanguage,
            String locale,
            String timezone,
            List<ScimEmail> emails) {
        return new ScimUserProfile(
                userName, name, displayName, preferredLanguage, locale, timezone, true, emails);
    }
}
