package com.example.backend.scim.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One value of the multi-valued {@code emails} attribute.
 *
 * <p>{@code type} is nullable because RFC 7643 does not require one, and
 * {@code primary} is a plain boolean because "not primary" and "absent" are the
 * same thing for a flag whose only meaning is which value is preferred.
 *
 * @param value   the address
 * @param type    'work', 'home', 'other', or {@code null} when unspecified
 * @param primary whether this is the preferred address
 */
public record ScimEmail(String value, String type, boolean primary) {

    /**
     * The canonical form of a submitted list: {@code (type, value)} duplicates
     * removed, and at most one primary.
     *
     * <p>Both rules are RFC 7643's and both are applied here rather than in the
     * web adapter, because they decide what is STORED: the unique index on
     * {@code scim_user_emails} would otherwise refuse a request a conformant
     * client is allowed to make, and "at most one primary" would be a constraint
     * violation rather than a resolved value.
     *
     * <p>De-duplication keeps the FIRST occurrence and its position, so the order
     * the connector sent survives. Where duplicates disagree about {@code primary}
     * the retained copy wins, which is the same rule stated once: the first
     * occurrence is the value.
     *
     * <p>Where more than one distinct address claims primary the FIRST claim is
     * kept and the rest are demoted, rather than the request being refused. A
     * refusal would be defensible, but the attribute's purpose is to name a
     * preference and the first stated preference is an answer; rejecting the whole
     * resource over it would fail a create that has one unambiguous reading.
     */
    public static List<ScimEmail> canonical(List<ScimEmail> submitted) {
        if (submitted == null || submitted.isEmpty()) {
            return List.of();
        }
        Map<String, ScimEmail> byIdentity = new LinkedHashMap<>();
        for (ScimEmail email : submitted) {
            byIdentity.putIfAbsent(identity(email), email);
        }
        List<ScimEmail> canonical = new ArrayList<>(byIdentity.size());
        boolean primaryTaken = false;
        for (ScimEmail email : byIdentity.values()) {
            if (email.primary() && !primaryTaken) {
                primaryTaken = true;
                canonical.add(email);
            } else if (email.primary()) {
                canonical.add(new ScimEmail(email.value(), email.type(), false));
            } else {
                canonical.add(email);
            }
        }
        return List.copyOf(canonical);
    }

    /**
     * What makes two values the same value: the type, case-insensitively because a
     * type is a keyword rather than data, and the address exactly as sent.
     *
     * <p>The address is compared case-sensitively on purpose. The local part of an
     * email address is case-sensitive per RFC 5321, and this service is not the
     * authority that decides two differently-cased addresses reach the same
     * mailbox; treating them as one value would silently discard data the
     * connector sent.
     */
    private static String identity(ScimEmail email) {
        String type = email.type() == null ? "" : email.type().toLowerCase(Locale.ROOT);
        return type + '\u0000' + email.value();
    }
}
