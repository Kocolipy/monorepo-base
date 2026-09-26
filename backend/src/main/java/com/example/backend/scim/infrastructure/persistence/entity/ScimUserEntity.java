package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Database representation of a SCIM User: the profile half of a resource whose
 * identity and version live in {@code scim_resources}.
 *
 * <p>The two tables are one aggregate, and {@code @MapsId} is what says so: the User's
 * primary key IS the resource's id, taken from the association rather than assigned
 * twice, so there is no way to write a User row whose key disagrees with its resource
 * row. {@code cascade = PERSIST} lets the pair be written by one {@code save} in the
 * right order — the resource first, because the User's foreign key points at it.
 *
 * <p>The association is eager because every read of a User renders {@code meta} and
 * the ETag, both of which come from the resource row: making it lazy would turn every
 * single read into two queries and every listing into N+1, for data that is never not
 * wanted.
 */
@Entity
@Table(name = "scim_users")
public class ScimUserEntity {

    @Id
    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;

    /**
     * The resource row carrying this User's id, version and timestamps.
     *
     * <p>{@code @MapsId} derives {@link #resourceId} from it, so the id is stated once.
     */
    @MapsId
    @OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "resource_id", nullable = false, updatable = false)
    private ScimResourceEntity resource;

    @Column(nullable = false, length = 256)
    private String userName;

    /**
     * The normalized form uniqueness is decided on, stored rather than computed in the
     * index: the normalization rule belongs to the domain, and a functional index would
     * be a second implementation of it in SQL that could drift.
     */
    @Column(nullable = false, length = 256, unique = true)
    private String normalizedUserName;

    /** Nullable: a credentialless User is a supported state, not an incomplete one. */
    @Column(length = 256)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 256)
    private String displayName;

    @Column(length = 256)
    private String formattedName;

    @Column(length = 256)
    private String familyName;

    @Column(length = 256)
    private String givenName;

    @Column(length = 256)
    private String middleName;

    @Column(length = 256)
    private String honorificPrefix;

    @Column(length = 256)
    private String honorificSuffix;

    @Column(length = 64)
    private String preferredLanguage;

    @Column(length = 64)
    private String locale;

    @Column(length = 64)
    private String timezone;

    /**
     * The multi-valued {@code emails} attribute, in the order the connector sent.
     *
     * <p>{@code @OrderColumn} rather than an unordered collection, because SCIM renders
     * the values back and a set that reshuffled between reads would make a
     * diff-based client rewrite the resource forever. Eager for the same reason the
     * resource association is: every rendering of a User needs it.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "scim_user_emails",
            joinColumns = @JoinColumn(name = "resource_id", nullable = false))
    @OrderColumn(name = "ordinal", nullable = false)
    private List<ScimUserEmailValue> emails = new ArrayList<>();

    protected ScimUserEntity() {
    }

    public ScimUserEntity(
            ScimResourceEntity resource,
            String userName,
            String normalizedUserName,
            String passwordHash,
            boolean active,
            String displayName,
            String formattedName,
            String familyName,
            String givenName,
            String middleName,
            String honorificPrefix,
            String honorificSuffix,
            String preferredLanguage,
            String locale,
            String timezone,
            List<ScimUserEmailValue> emails) {
        this.resource = resource;
        this.userName = userName;
        this.normalizedUserName = normalizedUserName;
        this.passwordHash = passwordHash;
        this.active = active;
        this.displayName = displayName;
        this.formattedName = formattedName;
        this.familyName = familyName;
        this.givenName = givenName;
        this.middleName = middleName;
        this.honorificPrefix = honorificPrefix;
        this.honorificSuffix = honorificSuffix;
        this.preferredLanguage = preferredLanguage;
        this.locale = locale;
        this.timezone = timezone;
        this.emails = new ArrayList<>(emails);
    }

    public ScimResourceEntity getResource() {
        return resource;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getUserName() {
        return userName;
    }

    public String getNormalizedUserName() {
        return normalizedUserName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public String getLocale() {
        return locale;
    }

    public String getTimezone() {
        return timezone;
    }

    public List<ScimUserEmailValue> getEmails() {
        return List.copyOf(emails);
    }
}
