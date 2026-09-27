-- The Group half of the normalized SCIM resource model, and the marker that makes
-- a resource a protected one.
--
-- Groups join `scim_resources` rather than generating ids of their own, which is
-- what V5 said this migration would do: the shared id namespace RFC 7643 requires
-- is a primary key there, and inheriting it is how a Group and a User are
-- guaranteed never to collide. The version lives there too, so a conditional write
-- locks the same row whichever kind of resource it is writing.

CREATE TABLE scim_groups (
    -- The resource id IS the Group's primary key, exactly as it is a User's: a
    -- Group is a resource, not a row that points at one.
    resource_id             UUID         NOT NULL,

    -- As the connector sent it, case and all. What is rendered back.
    display_name            VARCHAR(256) NOT NULL,

    -- The normalized form uniqueness is decided on, stored rather than computed in
    -- an index for the reason `scim_users.normalized_user_name` is: the
    -- normalization rule belongs to the domain, and a functional index would be a
    -- second implementation of it in SQL that could drift from the first.
    normalized_display_name VARCHAR(256) NOT NULL,

    CONSTRAINT pk_scim_groups PRIMARY KEY (resource_id),

    -- RFC 7643 does not require `displayName` to be unique. It is made unique here
    -- anyway, because this directory derives AUTHORITY from a Group: two Groups a
    -- human reads as the same name is how an administrator grants membership of the
    -- wrong one. Case-insensitively, so the two cannot differ only in case.
    CONSTRAINT uq_scim_groups_normalized_display_name UNIQUE (normalized_display_name),

    CONSTRAINT fk_scim_groups_resource
        FOREIGN KEY (resource_id) REFERENCES scim_resources (id) ON DELETE CASCADE
);

-- Membership: direct Users only, and that is a property of the schema rather than
-- of a validation pass.
--
-- `user_id` references `scim_users`, NOT `scim_resources`. The difference is the
-- whole rule: a foreign key to the resource table would accept a Group's id and
-- make nested Groups representable, and it would accept any id that exists at all.
-- Pointing at the User table means "a Group as a member", "an unknown id" and "a
-- deleted User" are all the same refusal — a foreign-key violation — with no
-- prior read that two concurrent writes could both pass.
CREATE TABLE scim_group_members (
    group_id UUID NOT NULL,

    user_id  UUID NOT NULL,

    -- The pair is the key, so a User cannot be added to the same Group twice and
    -- there is no second identifier for a membership to be looked up by. A
    -- membership has no attributes of its own: SCIM's `members` sub-attributes are
    -- rendered from the referenced resource, not stored per row.
    CONSTRAINT pk_scim_group_members PRIMARY KEY (group_id, user_id),

    CONSTRAINT fk_scim_group_members_group
        FOREIGN KEY (group_id) REFERENCES scim_groups (resource_id) ON DELETE CASCADE,

    CONSTRAINT fk_scim_group_members_user
        FOREIGN KEY (user_id) REFERENCES scim_users (resource_id) ON DELETE CASCADE
);

-- The reverse view every User read renders: "which Groups is this User in".
-- The primary key already indexes (group_id, user_id), which serves the forward
-- direction; this serves the reverse one, which is read on every single User
-- retrieval and would otherwise be a sequential scan of every membership in the
-- directory.
CREATE INDEX ix_scim_group_members_user ON scim_group_members (user_id);

-- What makes a resource protected, for both kinds at once.
--
-- The deployment's recovery identity and its Admin Group cannot be renamed,
-- rewritten or deleted, and the Bootstrap Admin's membership of that Group cannot
-- be removed. Those refusals need to recognise the resources they are about, and
-- recognising them by `userName` or `displayName` would mean the protection
-- depended on an attribute — which, for any other resource, is mutable. A marker
-- on the resource row does not: it is set once by seeding and there is no write
-- path that changes it.
--
-- Nullable, and NULL is the ordinary case: almost every resource is unprotected.
-- Non-null IS the protection, so "is this protected" is one column test that reads
-- the same for a User and for a Group.
ALTER TABLE scim_resources
    ADD COLUMN reserved_name VARCHAR(32);

-- Unique so seeding is idempotent against the database rather than against a prior
-- read: a restart that races another instance cannot produce two Admin Groups,
-- because the second INSERT violates this.
ALTER TABLE scim_resources
    ADD CONSTRAINT uq_scim_resources_reserved_name UNIQUE (reserved_name);

-- A closed set, checked here as well as in the domain enum, because the value
-- decides whether writes are refused: a typo that stored 'admin_group' would
-- silently leave the Admin Group unprotected and its authority underivable.
ALTER TABLE scim_resources
    ADD CONSTRAINT ck_scim_resources_reserved_name
        CHECK (reserved_name IS NULL
               OR reserved_name IN ('bootstrap-admin', 'admin-group'));

COMMENT ON COLUMN scim_resources.reserved_name IS
    'Names a resource the deployment reserves for recovery: the Bootstrap Admin '
    'User or the Admin Group. Non-null means every write and delete against the '
    'resource is refused. NULL for every ordinary resource.';

-- V3 created backend_app as a least-privilege runtime role holding grants per
-- table, and said a later migration adding a table the application writes must
-- grant it. These are those tables.
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_groups        TO backend_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_group_members TO backend_app;
