package com.example.backend.scim.domain;

import java.util.List;
import java.util.Optional;

/**
 * One attribute this service implements, with the characteristics RFC 7643 §7 gives it.
 *
 * <p>These are the facts every protocol path shares: discovery advertises them, a write reads
 * by them, a projection validates against them, and a filter or sort compares by them. How each
 * path ACTS on a fact stays with that path — this record says {@code groups} is read-only, and
 * the PUT reader ignoring it while a PATCH naming it is refused are the two readers' own rules.
 *
 * <p>A record with named factories rather than a builder, because the combinations that occur
 * are few and each factory names one of them: a singular value, a complex value, a
 * multi-valued complex value. Every factory starts from RFC 7643 §2.2's defaults — not
 * required, case-insensitive, no uniqueness, no canonical values — and the modifiers state each
 * departure, so a schema reads as the places this service differs from "an ordinary attribute".
 *
 * @param canonicalValues suggested values; empty when there are none
 * @param referenceTypes  the resource types a {@code reference} may name; empty otherwise
 */
public record ScimAttribute(
        String name,
        ScimAttributeType type,
        boolean multiValued,
        boolean required,
        boolean caseExact,
        Mutability mutability,
        Returned returned,
        Uniqueness uniqueness,
        List<ScimAttribute> subAttributes,
        List<String> canonicalValues,
        List<String> referenceTypes) {

    /** RFC 7643 §7 {@code mutability}, as far as this service uses it. */
    public enum Mutability {
        READ_ONLY,
        READ_WRITE,
        WRITE_ONLY
    }

    /** RFC 7643 §7 {@code returned}, as far as this service uses it. */
    public enum Returned {
        ALWAYS,
        DEFAULT,
        NEVER
    }

    /** RFC 7643 §7 {@code uniqueness}, as far as this service uses it. */
    public enum Uniqueness {
        NONE,
        SERVER
    }

    public ScimAttribute {
        subAttributes = List.copyOf(subAttributes);
        canonicalValues = List.copyOf(canonicalValues);
        referenceTypes = List.copyOf(referenceTypes);
    }

    /** A single-valued simple attribute with RFC 7643 §2.2's defaults. */
    public static ScimAttribute singular(
            String name, ScimAttributeType type, Mutability mutability, Returned returned) {
        return new ScimAttribute(name, type, false, false, false, mutability, returned,
                Uniqueness.NONE, List.of(), List.of(), List.of());
    }

    /** A single-valued complex attribute. */
    public static ScimAttribute complex(
            String name, Mutability mutability, Returned returned, List<ScimAttribute> sub) {
        return new ScimAttribute(name, ScimAttributeType.COMPLEX, false, false, false,
                mutability, returned, Uniqueness.NONE, sub, List.of(), List.of());
    }

    /** A multi-valued complex attribute. */
    public static ScimAttribute multiValuedComplex(
            String name, Mutability mutability, Returned returned, List<ScimAttribute> sub) {
        return new ScimAttribute(name, ScimAttributeType.COMPLEX, true, false, false,
                mutability, returned, Uniqueness.NONE, sub, List.of(), List.of());
    }

    /** A departure from the RFC 7643 §2.2 default: the attribute must have a value. */
    public ScimAttribute asRequired() {
        return new ScimAttribute(name, type, multiValued, true, caseExact, mutability, returned,
                uniqueness, subAttributes, canonicalValues, referenceTypes);
    }

    /** A departure from the RFC 7643 §2.2 default: values compare case-sensitively. */
    public ScimAttribute asCaseExact() {
        return new ScimAttribute(name, type, multiValued, required, true, mutability, returned,
                uniqueness, subAttributes, canonicalValues, referenceTypes);
    }

    /** A departure from the RFC 7643 §2.2 default: two resources may not share a value. */
    public ScimAttribute unique(Uniqueness scope) {
        return new ScimAttribute(name, type, multiValued, required, caseExact, mutability,
                returned, scope, subAttributes, canonicalValues, referenceTypes);
    }

    /** Suggested values, advertised without being enforced. */
    public ScimAttribute canonical(String... values) {
        return new ScimAttribute(name, type, multiValued, required, caseExact, mutability,
                returned, uniqueness, subAttributes, List.of(values), referenceTypes);
    }

    /** The resource types a reference may name. */
    public ScimAttribute references(String... resourceTypes) {
        return new ScimAttribute(name, type, multiValued, required, caseExact, mutability,
                returned, uniqueness, subAttributes, canonicalValues, List.of(resourceTypes));
    }

    /** Whether a write may assert this attribute: anything not read-only, a credential included. */
    public boolean isWritable() {
        return mutability != Mutability.READ_ONLY;
    }

    /**
     * Whether any rendering may include this attribute. An attribute that is never returned is
     * also never queryable: a filter or a sort on it would disclose what a read withholds.
     */
    public boolean isReturned() {
        return returned != Returned.NEVER;
    }

    /** The sub-attribute with this name, matched case-insensitively as RFC 7644 §3.10 requires. */
    public Optional<ScimAttribute> subAttribute(String subName) {
        return subAttributes.stream()
                .filter(sub -> sub.name().equalsIgnoreCase(subName))
                .findFirst();
    }
}
