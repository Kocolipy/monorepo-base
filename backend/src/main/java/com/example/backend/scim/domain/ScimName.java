package com.example.backend.scim.domain;

/**
 * The {@code name} complex attribute: every sub-attribute this service supports,
 * each of them optional.
 *
 * <p>A record with six nullable components rather than a map, because the
 * supported sub-attributes are exactly these six and a map would make
 * {@code name.nickName} — which this service does not support — expressible.
 *
 * <p>{@link #NONE} is the unassigned value, and {@link #isUnassigned()} is how a
 * renderer decides whether to emit the attribute at all: SCIM omits an unassigned
 * attribute rather than rendering an empty object, and a complex value all of
 * whose sub-attributes are absent is unassigned.
 *
 * @param formatted       the full name, as the connector would display it
 * @param familyName      surname
 * @param givenName       first name
 * @param middleName      middle name
 * @param honorificPrefix title preceding the name
 * @param honorificSuffix suffix following the name
 */
public record ScimName(
        String formatted,
        String familyName,
        String givenName,
        String middleName,
        String honorificPrefix,
        String honorificSuffix) {

    /** No name recorded at all. */
    public static final ScimName NONE = new ScimName(null, null, null, null, null, null);

    /** Whether every sub-attribute is absent, in which case SCIM omits the attribute. */
    public boolean isUnassigned() {
        return formatted == null
                && familyName == null
                && givenName == null
                && middleName == null
                && honorificPrefix == null
                && honorificSuffix == null;
    }
}
