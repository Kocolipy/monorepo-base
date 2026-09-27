package com.example.backend.scim.domain;

/**
 * A write could not be applied to the resource as it currently stands — a PATCH operation, or a
 * PUT asserting a changed {@code externalId}.
 *
 * <p>Raised while the operations are folded onto the stored resource — which is the only place
 * these refusals can be decided, because whether a filter matches anything depends on the stored
 * values. Nothing has been written when it is raised: the fold is computed in memory and written
 * once, so a failed operation anywhere in the sequence leaves every earlier one unapplied.
 *
 * <p>Carries the SCIM refusal it is and a detail built from this service's own vocabulary
 * (attribute names), never a submitted value.
 */
public class ScimPatchRefusedException extends RuntimeException {

    /** Which RFC 7644 §3.12 {@code scimType} the refusal is. */
    public enum Reason {
        /** The operation would remove a required attribute or a required sub-attribute. */
        MUTABILITY,
        /** A filtered path matched no value. */
        NO_TARGET
    }

    private final Reason reason;

    public ScimPatchRefusedException(Reason reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
