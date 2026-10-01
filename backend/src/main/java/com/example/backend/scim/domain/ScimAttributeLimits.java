package com.example.backend.scim.domain;

/**
 * The longest value each stored SCIM attribute accepts, and the check that refuses a longer one
 * before anything is written.
 *
 * <p>The numbers are the {@code VARCHAR} widths of the columns in the Flyway migrations, and
 * this class exists so a value that does not fit is refused as what it is — a {@code 400
 * invalidValue} naming the attribute and its limit — rather than reaching the database, whose
 * refusal is an integrity violation indistinguishable, from outside, from a conflict or a fault.
 * RFC 7643 has no {@code maxLength} characteristic, so the limits are advertised in
 * {@code docs/openapi.yaml} and in the refusal's {@code detail}, not in {@code /Schemas}.
 *
 * <p>Lengths are counted in Unicode code points, which is what PostgreSQL's
 * {@code VARCHAR(n)} counts in a UTF-8 database. Counting UTF-16 units instead would refuse a
 * 256-character name made of supplementary characters that the column stores without
 * complaint.
 *
 * <p>The two unique names are also checked in their NORMALIZED form, because that is a column
 * of the same width too: NFKC can expand a character into several (U+FDFA becomes eighteen), so
 * a {@code userName} that fits can normalize into one that does not.
 */
public final class ScimAttributeLimits {

    /** {@code scim_users.user_name} and {@code normalized_user_name}. */
    public static final int USER_NAME = 256;

    /** {@code scim_users.display_name}, and a Group's {@code display_name} and normalized form. */
    public static final int DISPLAY_NAME = 256;

    /** {@code scim_external_ids.external_id}, for Users and Groups alike. */
    public static final int EXTERNAL_ID = 256;

    /** Each {@code name} sub-attribute's column. */
    public static final int NAME_PART = 256;

    /** {@code preferred_language}, {@code locale} and {@code timezone}. */
    public static final int LOCALIZATION = 64;

    /** {@code scim_user_emails.value}. */
    public static final int EMAIL_VALUE = 256;

    /** {@code scim_user_emails.type}. */
    public static final int EMAIL_TYPE = 32;

    private ScimAttributeLimits() {
    }

    /**
     * Refuses a User profile holding any value longer than its column.
     *
     * @throws ScimValueTooLongException naming the first attribute found too long
     */
    public static void requireWithin(ScimUserProfile profile) {
        require("userName", profile.userName(), USER_NAME);
        require("userName", profile.normalizedUserName().value(), USER_NAME);
        ScimName name = profile.name();
        require("name.formatted", name.formatted(), NAME_PART);
        require("name.familyName", name.familyName(), NAME_PART);
        require("name.givenName", name.givenName(), NAME_PART);
        require("name.middleName", name.middleName(), NAME_PART);
        require("name.honorificPrefix", name.honorificPrefix(), NAME_PART);
        require("name.honorificSuffix", name.honorificSuffix(), NAME_PART);
        require("displayName", profile.displayName(), DISPLAY_NAME);
        require("preferredLanguage", profile.preferredLanguage(), LOCALIZATION);
        require("locale", profile.locale(), LOCALIZATION);
        require("timezone", profile.timezone(), LOCALIZATION);
        for (ScimEmail email : profile.emails()) {
            require("emails.value", email.value(), EMAIL_VALUE);
            require("emails.type", email.type(), EMAIL_TYPE);
        }
    }

    /**
     * Refuses a Group {@code displayName} longer than its column, as submitted or normalized.
     *
     * @throws ScimValueTooLongException naming {@code displayName}
     */
    public static void requireGroupDisplayNameWithin(String displayName) {
        require("displayName", displayName, DISPLAY_NAME);
        require("displayName", NormalizedDisplayName.of(displayName).value(), DISPLAY_NAME);
    }

    /**
     * Refuses an {@code externalId} longer than its column. An absent one is within any limit.
     *
     * @throws ScimValueTooLongException naming {@code externalId}
     */
    public static void requireExternalIdWithin(String externalId) {
        require("externalId", externalId, EXTERNAL_ID);
    }

    private static void require(String attribute, String value, int limit) {
        if (value != null && value.codePointCount(0, value.length()) > limit) {
            throw new ScimValueTooLongException(attribute, limit);
        }
    }
}
