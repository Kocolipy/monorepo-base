package com.example.backend.scim.domain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The {@code If-Match} precondition a connector sent with a write against an existing resource,
 * held unevaluated until the resource it names has been found.
 *
 * <p>Unevaluated on purpose. The specification plan orders the checks — authorization, then
 * existence, then the precondition — so a malformed or stale header against an id that names
 * nothing is a {@code 404}, not a {@code 400} or {@code 412}: answering the precondition first
 * would tell a caller that an id exists by the shape of the refusal. So the adapter only captures
 * what was sent, and the use case calls {@link #requireSatisfiedBy(long)} once it holds the
 * resource.
 *
 * <p>The precondition is OPTIONAL, as RFC 7644 §3.14 makes it for clients, because the
 * mainstream provisioning clients do not send it and refusing them would make provisioning
 * create-only. When it IS sent, exactly one strong entity tag is accepted, and it must be the
 * resource's current one:
 *
 * <ul>
 *   <li>no header at all is an unconditional write: it is applied, last writer wins. It is still
 *       applied under the resource lock in one transaction with its version advance, so two
 *       unconditional writers serialize rather than interleave — what they give up is only the
 *       refusal of a write based on a stale read;
 *   <li>{@code *}, a list of tags, more than one header, and anything that is not an entity tag
 *       are {@link InvalidPreconditionException} ({@code 400 invalidValue}): {@code *} would
 *       turn the check off, and a list lets a client assert "any of these", which is not exact;
 *   <li>a well-formed tag that is not the current version — including a weak {@code W/} tag,
 *       which {@code If-Match}'s strong comparison never matches — is
 *       {@link PreconditionFailedException} ({@code 412}).
 * </ul>
 *
 * <p>This service's tags are the decimal version in quotes ({@code "7"}), so a tag holding
 * anything else is well-formed but cannot be current, and is a {@code 412} like any stale one.
 *
 * @param headerValues every {@code If-Match} header value the request carried, in order; empty
 *                     when there were none
 */
public record ScimVersionPrecondition(List<String> headerValues) {

    /**
     * An entity tag as RFC 9110 §8.8.3 spells it: optional weak prefix, then a quoted string of
     * {@code etagc} characters. Anything else — a bare token, an unterminated quote, a list — is
     * not one tag.
     */
    private static final Pattern ENTITY_TAG =
            Pattern.compile("(W/)?\"[\\x21\\x23-\\x7E\\x80-\\xFF]*\"");

    public ScimVersionPrecondition {
        headerValues = headerValues == null ? List.of() : List.copyOf(headerValues);
    }

    /** The precondition carried by these header values. */
    public static ScimVersionPrecondition ofIfMatch(List<String> headerValues) {
        return new ScimVersionPrecondition(headerValues);
    }

    /** Whether a validator was sent at all; a write without one is unconditional. */
    public boolean isConditional() {
        return !headerValues.isEmpty();
    }

    /**
     * Refuses the write unless the precondition is absent or names exactly this version.
     *
     * @throws InvalidPreconditionException  when it was {@code *}, a list, repeated, or not an
     *                                       entity tag
     * @throws PreconditionFailedException   when it is one well-formed tag that is not this
     *                                       version's
     */
    public void requireSatisfiedBy(long currentVersion) {
        if (!isConditional()) {
            return;
        }
        if (headerValues.size() != 1) {
            throw new InvalidPreconditionException();
        }
        String tag = headerValues.get(0).strip();
        if (!ENTITY_TAG.matcher(tag).matches()) {
            throw new InvalidPreconditionException();
        }
        if (!tag.equals("\"" + currentVersion + "\"")) {
            throw new PreconditionFailedException();
        }
    }
}
