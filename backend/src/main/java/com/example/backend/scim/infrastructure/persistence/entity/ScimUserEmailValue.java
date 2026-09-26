package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One row of {@code scim_user_emails}, as an element of the owning User's collection.
 *
 * <p>An {@code @Embeddable} rather than an entity because an email value has no
 * identity of its own: it is part of the User's representation, it is written and
 * replaced with the User, and nothing ever refers to one. Mapping it as an entity
 * would give it a primary key that no caller would ever use and a lifecycle that could
 * drift from its owner's.
 */
@Embeddable
public class ScimUserEmailValue {

    @Column(nullable = false, length = 256)
    private String value;

    /** Nullable: RFC 7643 does not require a type. */
    @Column(length = 32)
    private String type;

    /**
     * Named {@code is_primary} in the schema because {@code primary} is a reserved
     * word in enough SQL dialects to be worth avoiding, and mapped explicitly here so
     * the Java name can stay the attribute's own.
     */
    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    protected ScimUserEmailValue() {
    }

    public ScimUserEmailValue(String value, String type, boolean primary) {
        this.value = value;
        this.type = type;
        this.primary = primary;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public boolean isPrimary() {
        return primary;
    }
}
