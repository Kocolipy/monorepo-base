-- The privacy-minimal record a SCIM DELETE leaves behind.
--
-- A deleted User or Group is removed from `scim_resources`, and the cascades there
-- remove everything readable about it: the profile, emails, credential, password
-- history, memberships and connector aliases. What survives is this row, and it is
-- deliberately incapable of holding any of those. Its columns are a UUID, a resource
-- type constrained to the two names the directory issues, and a timestamp; there is no
-- text column a profile value, a credential or a member id could be written into.
-- `ScimDeletionIntegrationTests` asserts exactly that column set, so a later
-- migration adding a free-text column here fails the build rather than a review.
--
-- Never consulted for uniqueness. A former `userName`, `displayName` or connector
-- `externalId` is reusable immediately, per RFC 7644 §3.6; audit resolves every
-- subject by this stable id, so a reused name cannot merge two identities in the trail.
--
-- Retained independently of audit retention, so the application role may insert and
-- read tombstones but neither change nor remove one.
CREATE TABLE scim_tombstones (
    resource_id   UUID        NOT NULL,
    resource_type VARCHAR(16) NOT NULL,
    -- When the resource was deleted, UTC.
    deleted_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_scim_tombstones PRIMARY KEY (resource_id),
    CONSTRAINT ck_scim_tombstones_resource_type
        CHECK (resource_type IN ('User', 'Group'))
);

GRANT SELECT, INSERT ON scim_tombstones TO backend_app;
