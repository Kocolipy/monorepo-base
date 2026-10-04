-- Connector tokens carry Permissions instead of an access level (#117, ADR 0010).
--
-- A token's READ_ONLY / READ_WRITE scope could not say "this connector manages
-- Groups but not Users". It is replaced by the set of Permissions the token
-- carries, from the same vocabulary a User's Roles grant, spelled as the wire
-- spells them ('user:read').
--
-- No data conversion: there is no production deployment. A development
-- database's existing tokens get an EMPTY set, which authenticates and may read
-- discovery but nothing else — the safe reading of a credential whose intended
-- Permissions were never recorded. Reissue them. A forward migration, never an
-- edit of an applied script.
ALTER TABLE scim_connector_tokens ADD COLUMN permissions TEXT[] NOT NULL DEFAULT '{}';
ALTER TABLE scim_connector_tokens ALTER COLUMN permissions DROP DEFAULT;

-- A token can carry only the four directory Permissions: the rest of the
-- vocabulary guards the application chain, which no bearer token reaches. The
-- application refuses the others first; this is the last line, so a write path
-- that forgot the rule cannot store a token that would mean more the day a route
-- began to honour it.
ALTER TABLE scim_connector_tokens
    ADD CONSTRAINT ck_scim_connector_tokens_directory_permissions
        CHECK (permissions <@ ARRAY['user:read', 'user:write', 'group:read', 'group:write']::TEXT[]
               AND array_position(permissions, NULL) IS NULL);

ALTER TABLE scim_connector_tokens DROP COLUMN scope;

-- The Permissions a token was issued or rotated with, or — on a refused
-- escalation — the Permissions that were asked for. Names from the closed
-- vocabulary in code, comma-joined as changed_paths is, never a value a caller
-- submitted unchecked: a name that is no Permission is refused before anything is
-- recorded. NULL for every other event. The table-level grants in V1 already
-- cover a new column, and the append-only trigger refuses an UPDATE of it.
ALTER TABLE audit_events ADD COLUMN permissions TEXT;
