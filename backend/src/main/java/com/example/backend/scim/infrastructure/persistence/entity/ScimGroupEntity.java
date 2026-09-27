package com.example.backend.scim.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Database representation of a SCIM Group: the naming half of a resource whose identity
 * and version live in {@code scim_resources}.
 *
 * <p>The two tables are one aggregate, and {@code @MapsId} is what says so — the Group's
 * primary key IS the resource's id, taken from the association rather than assigned
 * twice, exactly as a User's is. {@code cascade = PERSIST} lets the pair be written by one
 * {@code save} in the right order, the resource first because the Group's foreign key
 * points at it.
 *
 * <p><strong>Membership is deliberately not mapped here.</strong> A Group's members are
 * rendered with a {@code display} label that lives on the referenced User's row, so an
 * association to {@code ScimUserEntity} would have to load whole Users — each with its own
 * eager resource row and email collection — to render two fields per member. The adapter
 * reads memberships through an explicit projection over
 * {@link ScimGroupMemberEntity} joined to the User table instead, which fetches exactly
 * the id and the label in one query. It also keeps a Group read from being a route to a
 * User's credential.
 */
@Entity
@Table(name = "scim_groups")
public class ScimGroupEntity {

    @Id
    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;

    /**
     * The resource row carrying this Group's id, version and timestamps.
     *
     * <p>{@code @MapsId} derives {@link #resourceId} from it, so the id is stated once.
     * Eager because every read of a Group renders {@code meta} and the ETag, both of which
     * come from the resource row: making it lazy would turn every single read into two
     * queries for data that is never not wanted.
     */
    @MapsId
    @OneToOne(optional = false, fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "resource_id", nullable = false, updatable = false)
    private ScimResourceEntity resource;

    /** As the connector sent it, case and all. What is rendered back. */
    @Column(name = "display_name", nullable = false, length = 256)
    private String displayName;

    /**
     * The normalized form uniqueness is decided on, stored rather than computed in the
     * index: the normalization rule belongs to the domain, and a functional index would be
     * a second implementation of it in SQL that could drift.
     */
    @Column(name = "normalized_display_name", nullable = false, length = 256, unique = true)
    private String normalizedDisplayName;

    protected ScimGroupEntity() {
    }

    public ScimGroupEntity(
            ScimResourceEntity resource, String displayName, String normalizedDisplayName) {
        this.resource = resource;
        this.displayName = displayName;
        this.normalizedDisplayName = normalizedDisplayName;
    }

    public ScimResourceEntity getResource() {
        return resource;
    }

    public String getDisplayName() {
        return displayName;
    }
}
