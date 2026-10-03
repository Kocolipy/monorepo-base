package com.example.backend.scim.domain;

import static com.example.backend.scim.domain.ScimAttribute.Mutability.READ_ONLY;
import static com.example.backend.scim.domain.ScimAttribute.Mutability.READ_WRITE;
import static com.example.backend.scim.domain.ScimAttribute.Mutability.WRITE_ONLY;
import static com.example.backend.scim.domain.ScimAttribute.Returned.ALWAYS;
import static com.example.backend.scim.domain.ScimAttribute.Returned.DEFAULT;
import static com.example.backend.scim.domain.ScimAttribute.Returned.NEVER;
import static com.example.backend.scim.domain.ScimAttributeType.BOOLEAN;
import static com.example.backend.scim.domain.ScimAttributeType.DATE_TIME;
import static com.example.backend.scim.domain.ScimAttributeType.REFERENCE;
import static com.example.backend.scim.domain.ScimAttributeType.STRING;

import com.example.backend.scim.domain.ScimAttribute.Uniqueness;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The attributes one resource type has, and the one place that says so.
 *
 * <p>Every protocol path reads its attribute facts from here — supported names, type, case
 * sensitivity, cardinality, mutability and returnability — so a fact changes once:
 *
 * <ul>
 *   <li>discovery renders {@link #attributes()} as the {@code /Schemas} document, so it
 *       advertises exactly what exists;
 *   <li>request reading accepts the writable names, ignores the read-only ones and refuses
 *       anything else, so an attribute this service cannot store is rejected rather than
 *       silently dropped;
 *   <li>projection validates requested paths against it, so {@code attributes=nickName} is a
 *       refusal rather than an empty resource;
 *   <li>a PATCH path is classified by it — unknown, read-only, simple or complex, multi-valued
 *       or not — before the PATCH reader decides what the operation does;
 *   <li>{@link ScimQueryVocabulary} derives each queryable path's comparison from it.
 * </ul>
 *
 * <p><strong>Common attributes are not schema attributes.</strong> RFC 7643 §3.1 gives every
 * resource {@code schemas}, {@code id}, {@code externalId} and {@code meta}; a schema document
 * lists the attributes the schema ADDS, so they are in {@link #COMMON} and not in
 * {@link #attributes()}, and discovery never advertises them as User or Group attributes.
 *
 * <p><strong>Unsupported core attributes are absent on purpose.</strong> RFC 7643's User schema
 * defines {@code nickName}, {@code title}, {@code phoneNumbers}, {@code addresses} and more; none
 * is stored here, so none is advertised, and asserting one is refused. The Enterprise User
 * extension is likewise absent — a service that advertised it and ignored it would be worse than
 * one that says it does not have it.
 */
public final class ScimResourceSchema {

    /**
     * The RFC 7643 §3.1 common attributes, which every resource has.
     *
     * <p>{@code schemas} and {@code id} are always returned, so a projection never removes them.
     * {@code meta} is returned by default and may be projected away — the version it carries is
     * also in the ETag header. {@code externalId} is the one writable common attribute, and is
     * written under the calling connector alone. Every value here is an identifier or a rendered
     * value, so all of them compare case-exactly: {@code meta.location} and {@code meta.version}
     * compare against the exact strings a client reads back.
     */
    public static final List<ScimAttribute> COMMON = List.of(
            new ScimAttribute("schemas", REFERENCE, true, true, true, READ_ONLY, ALWAYS,
                    Uniqueness.NONE, List.of(), List.of(), List.of()),
            ScimAttribute.singular("id", STRING, READ_ONLY, ALWAYS).asCaseExact(),
            ScimAttribute.singular("externalId", STRING, READ_WRITE, DEFAULT).asCaseExact(),
            ScimAttribute.complex("meta", READ_ONLY, DEFAULT, List.of(
                    ScimAttribute.singular("resourceType", STRING, READ_ONLY, DEFAULT)
                            .asCaseExact(),
                    ScimAttribute.singular("created", DATE_TIME, READ_ONLY, DEFAULT)
                            .asCaseExact(),
                    ScimAttribute.singular("lastModified", DATE_TIME, READ_ONLY, DEFAULT)
                            .asCaseExact(),
                    ScimAttribute.singular("location", REFERENCE, READ_ONLY, DEFAULT)
                            .asCaseExact(),
                    ScimAttribute.singular("version", STRING, READ_ONLY, DEFAULT)
                            .asCaseExact()))
                    .asCaseExact());

    /**
     * The core User schema's attributes, in RFC 7643's order.
     *
     * <p>Case-insensitive like every attribute below that does not say otherwise: RFC 7643 §2.2
     * makes {@code caseExact=false} the default, and §8.7.1 keeps it for every core User string
     * this service implements. The flag is what filters and sort apply too.
     */
    private static final List<ScimAttribute> USER_ATTRIBUTES = List.of(
            ScimAttribute.singular("userName", STRING, READ_WRITE, DEFAULT)
                    .asRequired()
                    .unique(Uniqueness.SERVER),
            ScimAttribute.complex("name", READ_WRITE, DEFAULT, List.of(
                    ScimAttribute.singular("formatted", STRING, READ_WRITE, DEFAULT),
                    ScimAttribute.singular("familyName", STRING, READ_WRITE, DEFAULT),
                    ScimAttribute.singular("givenName", STRING, READ_WRITE, DEFAULT),
                    ScimAttribute.singular("middleName", STRING, READ_WRITE, DEFAULT),
                    ScimAttribute.singular("honorificPrefix", STRING, READ_WRITE, DEFAULT),
                    ScimAttribute.singular("honorificSuffix", STRING, READ_WRITE, DEFAULT))),
            ScimAttribute.singular("displayName", STRING, READ_WRITE, DEFAULT),
            ScimAttribute.singular("preferredLanguage", STRING, READ_WRITE, DEFAULT),
            ScimAttribute.singular("locale", STRING, READ_WRITE, DEFAULT),
            ScimAttribute.singular("timezone", STRING, READ_WRITE, DEFAULT),
            ScimAttribute.singular("active", BOOLEAN, READ_WRITE, DEFAULT),
            // writeOnly and never returned, which is how RFC 7643 declares a credential — and the
            // whole reason `attributes=password` needs no special case, and why no filter or sort
            // can name it. Case-exact, departing from §8.7.1's example: a password is a secret
            // whose case is part of it, and no comparison here ever ignores that.
            ScimAttribute.singular("password", STRING, WRITE_ONLY, NEVER).asCaseExact(),
            ScimAttribute.multiValuedComplex("emails", READ_WRITE, DEFAULT, List.of(
                    ScimAttribute.singular("value", STRING, READ_WRITE, DEFAULT),
                    // Suggested, not enforced: RFC 7643 §2.3.1 lets a service restrict a type to
                    // its canonical values, and this one stores any label up to its length limit.
                    ScimAttribute.singular("type", STRING, READ_WRITE, DEFAULT)
                            .canonical("work", "home", "other"),
                    ScimAttribute.singular("primary", BOOLEAN, READ_WRITE, DEFAULT))),
            // The reverse membership view. Wholly readOnly — including its top level, which is what
            // distinguishes it from `members` on a Group: a Group's membership is written there and
            // only there, and this is the same relation seen from the other end. So every
            // sub-attribute is derived, and a submitted `groups` is ignored rather than stored.
            // `type` is `direct` for every entry, because this directory has no nested Groups and
            // so no indirect membership to report. `value` and `$ref` are a resource id and its
            // URI, and stay case-exact: RFC 7643 §2.3.7 makes a reference case-exact.
            ScimAttribute.multiValuedComplex("groups", READ_ONLY, DEFAULT, List.of(
                    ScimAttribute.singular("value", STRING, READ_ONLY, DEFAULT).asCaseExact(),
                    ScimAttribute.singular("display", STRING, READ_ONLY, DEFAULT),
                    ScimAttribute.singular("$ref", REFERENCE, READ_ONLY, DEFAULT)
                            .asCaseExact()
                            .references("Group"),
                    ScimAttribute.singular("type", STRING, READ_ONLY, DEFAULT)
                            .canonical("direct"))));

    /**
     * The core Group schema's attributes, in RFC 7643's order.
     *
     * <p>Deliberately short: a Group in this directory is a name and a membership, because what a
     * Group is FOR here is conferring authority. {@code members} is {@code readWrite} at the top
     * level with {@code readOnly} sub-attributes other than {@code value} — RFC 7643 §4.2's own
     * shape, which makes "{@code display} is ignored on write" an advertised fact.
     */
    private static final List<ScimAttribute> GROUP_ATTRIBUTES = List.of(
            // Server-unique, which RFC 7643 does not require: a Group's membership confers
            // authority, and two Groups an administrator reads as the same name is how membership
            // of the wrong one gets granted. Advertised, so a connector is told rather than
            // discovering it through a 409.
            ScimAttribute.singular("displayName", STRING, READ_WRITE, DEFAULT)
                    .asRequired()
                    .unique(Uniqueness.SERVER),
            ScimAttribute.multiValuedComplex("members", READ_WRITE, DEFAULT, List.of(
                    // The only sub-attribute a write supplies: the member's resource id, which like
                    // every id is case-exact.
                    ScimAttribute.singular("value", STRING, READ_WRITE, DEFAULT).asCaseExact(),
                    // Derived from the referenced User rather than stored, which is why a
                    // submitted value is ignored: there is no column it could be written to.
                    ScimAttribute.singular("display", STRING, READ_ONLY, DEFAULT),
                    // Only Users can be members — this directory has no nested Groups.
                    ScimAttribute.singular("$ref", REFERENCE, READ_ONLY, DEFAULT)
                            .asCaseExact()
                            .references("User"),
                    ScimAttribute.singular("type", STRING, READ_ONLY, DEFAULT)
                            .canonical("User"))));

    private static final ScimResourceSchema USER =
            new ScimResourceSchema(ScimResourceType.USER, USER_ATTRIBUTES);

    private static final ScimResourceSchema GROUP =
            new ScimResourceSchema(ScimResourceType.GROUP, GROUP_ATTRIBUTES);

    private final ScimResourceType type;

    private final List<ScimAttribute> attributes;

    private ScimResourceSchema(ScimResourceType type, List<ScimAttribute> attributes) {
        this.type = type;
        this.attributes = attributes;
    }

    /** The schema of one resource type. */
    public static ScimResourceSchema of(ScimResourceType type) {
        return type == ScimResourceType.USER ? USER : GROUP;
    }

    /** The resource type this is the schema of. */
    public ScimResourceType type() {
        return type;
    }

    /** The attributes this type's schema declares, in RFC order: what discovery advertises. */
    public List<ScimAttribute> attributes() {
        return attributes;
    }

    /** Every attribute a resource of this type has: the common ones, then the declared ones. */
    public List<ScimAttribute> allAttributes() {
        return Stream.concat(COMMON.stream(), attributes.stream()).toList();
    }

    /**
     * The attribute with this name — common or declared — matched case-insensitively, as RFC 7644
     * §3.10 requires of attribute names.
     */
    public Optional<ScimAttribute> find(String name) {
        return allAttributes().stream()
                .filter(attribute -> attribute.name().equalsIgnoreCase(name))
                .findFirst();
    }

    /** Every top-level attribute name, common and declared, in canonical spelling. */
    public Set<String> names() {
        return namesWhere(attribute -> true);
    }

    /** The names a write may assert, a write-only credential included. */
    public Set<String> writableNames() {
        return namesWhere(ScimAttribute::isWritable);
    }

    /** The names no write can change. */
    public Set<String> readOnlyNames() {
        return namesWhere(attribute -> !attribute.isWritable());
    }

    /** The names every rendering carries, which a projection therefore never removes. */
    public Set<String> alwaysReturnedNames() {
        return namesWhere(attribute -> attribute.returned() == ScimAttribute.Returned.ALWAYS);
    }

    private Set<String> namesWhere(Predicate<ScimAttribute> predicate) {
        return allAttributes().stream()
                .filter(predicate)
                .map(ScimAttribute::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
