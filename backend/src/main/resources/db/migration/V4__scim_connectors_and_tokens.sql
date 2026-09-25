-- Connector identity, its bearer tokens, and the connector-scoped externalId
-- aliases a connector's deletion has to take with it.
--
-- Three tables rather than two because the alias relation is what makes
-- "deleting a connector removes all of its aliases" a statement about data
-- rather than about intent. The resource the alias points at does not exist yet
-- — scim_resources arrives with the User and Group tables — so resource_id is
-- carried here as a plain UUID with no foreign key. The key is added by the
-- migration that creates the resource table; until then an alias row is written
-- only by a test, which is exactly enough for the deletion rule to be
-- observable.

CREATE TABLE scim_connectors (
    -- Assigned by the application before the INSERT, like accounts.id and
    -- audit_events.id, so every layer already agrees on the connector's identity
    -- before the row exists. It is also the id an audit event names, which must
    -- be decided before the event that records the creation is appended.
    id           UUID         NOT NULL,

    display_name VARCHAR(200) NOT NULL,

    created_at   TIMESTAMPTZ  NOT NULL,

    -- Deletion is a transition, not a DELETE. The row survives so an audit event
    -- that names this connector still resolves to something a year later, and so
    -- a token row's foreign key stays satisfiable. What deletion actually costs
    -- the connector is its tokens and its aliases, both removed in the same
    -- transaction that sets this column.
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT pk_scim_connectors PRIMARY KEY (id)
);

CREATE TABLE scim_connector_tokens (
    id                   UUID         NOT NULL,

    connector_id         UUID         NOT NULL,

    -- The non-secret half of the opaque bearer value, and the only part used to
    -- FIND the row. Looking a token up by its hash would work too, but it would
    -- make every presented value a query key and every failed lookup a timing
    -- signal about the hash space; a separate lookup id keeps the secret half
    -- out of the WHERE clause entirely.
    lookup_id            VARCHAR(64)  NOT NULL,

    -- SHA-256 of the COMPLETE presented value, never the secret half alone, and
    -- never the value itself. Unstretched is correct here only because the value
    -- is 256 bits of SecureRandom material rather than a password — see
    -- ConnectorTokenSecret, which says so at the one place that computes it.
    token_hash           BYTEA        NOT NULL,

    -- READ_ONLY or READ_WRITE; write implies read.
    scope                VARCHAR(16)  NOT NULL,

    issued_at            TIMESTAMPTZ  NOT NULL,

    -- When the token stops being accepted. Rotation may bring this FORWARD to
    -- end an overlap window early; nothing may move it back.
    expires_at           TIMESTAMPTZ  NOT NULL,

    -- The expiry the token was issued with, kept so the rule "rotation never
    -- extends an old token's lifetime" is checkable against the row itself
    -- rather than against the code that wrote it.
    original_expires_at  TIMESTAMPTZ  NOT NULL,

    revoked_at           TIMESTAMPTZ,

    -- Rotation lineage: which token replaced this one. Null on a token that was
    -- never rotated.
    replaced_by_token_id UUID,

    CONSTRAINT pk_scim_connector_tokens PRIMARY KEY (id),

    CONSTRAINT uq_scim_connector_tokens_lookup_id UNIQUE (lookup_id),

    CONSTRAINT fk_scim_connector_tokens_connector
        FOREIGN KEY (connector_id) REFERENCES scim_connectors (id),

    CONSTRAINT fk_scim_connector_tokens_replacement
        FOREIGN KEY (replaced_by_token_id) REFERENCES scim_connector_tokens (id),

    -- The lifetime rule as a database constraint. An overlap window that tried
    -- to run past the expiry the token was issued with is refused by the server,
    -- so the rule holds against a future code path that never read the policy
    -- class.
    CONSTRAINT ck_scim_connector_tokens_expiry_never_extended
        CHECK (expires_at <= original_expires_at)
);

-- Deleting a connector revokes every token it holds, and the Admin view lists a
-- connector's tokens; both read by connector.
CREATE INDEX ix_scim_connector_tokens_connector_id
    ON scim_connector_tokens (connector_id);

CREATE TABLE scim_external_ids (
    connector_id UUID         NOT NULL,

    -- The SCIM resource the alias names. No foreign key yet: scim_resources does
    -- not exist until the User and Group tables land.
    resource_id  UUID         NOT NULL,

    -- Case-exact, per RFC 7643. Duplicate values across resources within one
    -- connector are allowed, so there is no unique constraint on the value.
    external_id  VARCHAR(256) NOT NULL,

    CONSTRAINT pk_scim_external_ids PRIMARY KEY (connector_id, resource_id),

    CONSTRAINT fk_scim_external_ids_connector
        FOREIGN KEY (connector_id) REFERENCES scim_connectors (id)
);

-- Connector-scoped filtering on externalId, which is the only way this relation
-- is read on the request path.
CREATE INDEX ix_scim_external_ids_value
    ON scim_external_ids (connector_id, external_id);

-- V3 created backend_app as a least-privilege runtime role holding grants per
-- table, and said in as many words that a later migration adding a table the
-- application writes must grant it. These are those tables.
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_connectors       TO backend_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_connector_tokens TO backend_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON scim_external_ids     TO backend_app;
