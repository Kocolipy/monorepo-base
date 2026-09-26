-- The normalized SCIM resource model, and the User half of it.
--
-- `scim_resources` exists as a table of its own rather than as columns on
-- `scim_users` because the SCIM `id` namespace is shared: RFC 7643 requires an
-- id to be unique across every resource type, so a User and a Group may never
-- collide. Two tables each generating their own ids could only promise that by
-- trusting UUID randomness; one table holding the id, its type and its version
-- promises it with a primary key. The Group half joins the same table in a later
-- migration and inherits the guarantee rather than re-stating it.
--
-- The version lives here too, beside the id, because every resource type
-- versions the same way and conditional writes lock this row. Keeping it out of
-- the per-type tables is what lets a single statement take the lock whatever
-- kind of resource is being written.

CREATE TABLE scim_resources (
    -- Assigned by the application before the INSERT, like every other id in this
    -- schema, so the resource's identity is decided before the row exists and the
    -- audit event that records the creation can name it.
    id               UUID        NOT NULL,

    -- 'User' or 'Group', spelled as RFC 7643 spells the resource type, because
    -- this value is rendered into `meta.resourceType` unchanged.
    resource_type    VARCHAR(16) NOT NULL,

    -- Monotonically increasing from 1, rendered as the strong ETag and as
    -- `meta.version`. A BIGINT rather than a timestamp: a version has to change
    -- on every representation change even when two changes land in the same
    -- clock tick, which a timestamp cannot promise.
    version          BIGINT      NOT NULL,

    created_at       TIMESTAMPTZ NOT NULL,

    last_modified_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_scim_resources PRIMARY KEY (id),

    CONSTRAINT ck_scim_resources_type
        CHECK (resource_type IN ('User', 'Group')),

    -- A version below 1 would render an ETag no client could have been given.
    CONSTRAINT ck_scim_resources_version_positive
        CHECK (version >= 1)
);

CREATE TABLE scim_users (
    -- The resource id IS the user's primary key: a User is a resource, not a row
    -- that points at one, so there is no second identifier to keep in step.
    resource_id          UUID         NOT NULL,

    -- As the connector sent it, case and all. What is rendered back.
    user_name            VARCHAR(256) NOT NULL,

    -- The normalized form uniqueness is decided on, stored rather than computed
    -- in the index so the normalization rule lives in one place — the domain —
    -- and the database enforces the result instead of re-implementing the rule in
    -- SQL, where the two could drift.
    normalized_user_name VARCHAR(256) NOT NULL,

    -- Nullable: a credentialless User is a supported state. It cannot log in
    -- until a password is set, which is a fact about authentication rather than
    -- about whether the identity may exist.
    password_hash        VARCHAR(256),

    -- No column default. `active` defaults to true on SCIM create, and that
    -- default belongs to the create use case where the RFC puts it; a second copy
    -- here would apply to writes the use case never saw.
    active               BOOLEAN      NOT NULL,

    display_name         VARCHAR(256),

    -- The `name` complex attribute, flattened. One row per User rather than a
    -- sub-table, because the sub-attributes are single-valued and the whole
    -- complex value is written and read as a unit.
    formatted_name       VARCHAR(256),
    family_name          VARCHAR(256),
    given_name           VARCHAR(256),
    middle_name          VARCHAR(256),
    honorific_prefix     VARCHAR(256),
    honorific_suffix     VARCHAR(256),

    preferred_language   VARCHAR(64),
    locale               VARCHAR(64),
    timezone            VARCHAR(64),

    CONSTRAINT pk_scim_users PRIMARY KEY (resource_id),

    -- Uniqueness among live Users, case-insensitively, as RFC 7643 requires of
    -- `userName`. A deleted User's row is gone, which is what makes a former
    -- userName reusable.
    CONSTRAINT uq_scim_users_normalized_user_name UNIQUE (normalized_user_name),

    CONSTRAINT fk_scim_users_resource
        FOREIGN KEY (resource_id) REFERENCES scim_resources (id) ON DELETE CASCADE
);

CREATE TABLE scim_user_emails (
    resource_id UUID         NOT NULL,

    -- Position in the multi-valued attribute, so the order a connector sent is
    -- the order rendered back. SCIM does not require order to be preserved, but
    -- a listing that reshuffles on every read makes a diff-based client rewrite
    -- the resource forever.
    ordinal     INTEGER      NOT NULL,

    value       VARCHAR(256) NOT NULL,

    -- 'work', 'home', 'other' or absent; RFC 7643's canonical values are not a
    -- closed set for a type sub-attribute, so this is not constrained here.
    type        VARCHAR(32),

    is_primary  BOOLEAN      NOT NULL,

    CONSTRAINT pk_scim_user_emails PRIMARY KEY (resource_id, ordinal),

    CONSTRAINT fk_scim_user_emails_user
        FOREIGN KEY (resource_id) REFERENCES scim_users (resource_id) ON DELETE CASCADE
);

-- `(user, type, value)` uniqueness, with an absent type treated as a value of its
-- own rather than as "distinct from everything". A plain UNIQUE constraint would
-- not do it: in SQL two NULLs are never equal, so two typeless copies of the same
-- address would both be accepted. The domain de-duplicates before writing; this
-- index is what makes that a property of the data.
CREATE UNIQUE INDEX uq_scim_user_emails_type_value
    ON scim_user_emails (resource_id, coalesce(type, ''), value);

-- At most one primary email per User. A partial index rather than a trigger or a
-- check: the rule is "no two rows", which is what a unique index says.
CREATE UNIQUE INDEX uq_scim_user_emails_one_primary
    ON scim_user_emails (resource_id)
    WHERE is_primary;

-- The foreign key V4 deferred, now that the table it points at exists. V4's
-- comment promised this migration would add it.
--
-- ON DELETE CASCADE because an alias is meaningless without its resource: a
-- deleted User's aliases are gone for the same reason a deleted connector's are,
-- and leaving them behind would make a reused alias value resolve to a resource
-- that returns 404.
ALTER TABLE scim_external_ids
    ADD CONSTRAINT fk_scim_external_ids_resource
        FOREIGN KEY (resource_id) REFERENCES scim_resources (id) ON DELETE CASCADE;

-- V3 created backend_app as a least-privilege runtime role holding grants per
-- table, and said that a later migration adding a table the application writes
-- must grant it. These are those tables.
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_resources    TO backend_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_users        TO backend_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_user_emails  TO backend_app;
