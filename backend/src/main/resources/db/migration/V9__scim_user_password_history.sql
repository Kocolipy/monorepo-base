-- A User's password history: the hashes of its most recent passwords, kept only so
-- a password change can refuse one of them being set again.
--
-- The specification plan's password policy refuses a password that matches "any of
-- the User's last 3 passwords, the current one included", on every path that sets
-- one. That needs the previous hashes, and `scim_users.password_hash` holds only the
-- current one, so they live here.
--
-- Hashes only, and the same Argon2id hashes the credential is stored as: a candidate
-- is checked by matching it against each row through the password encoder, which is
-- the only way to compare a plaintext with a salted hash. Nothing here can be read
-- back as a password, and the application never tries.
--
-- The application trims the history to the newest three on every successful change,
-- so the table never holds more than the rule consults. It is deleted with the User
-- by the cascade below, so no deletion path has to remember it.
CREATE TABLE scim_user_password_history (
    id            UUID         NOT NULL,
    user_id       UUID         NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    -- When this password was set, UTC. What "most recent" is ordered by.
    set_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_scim_user_password_history PRIMARY KEY (id),
    CONSTRAINT fk_scim_user_password_history_user
        FOREIGN KEY (user_id) REFERENCES scim_users (resource_id) ON DELETE CASCADE
);

-- Every read and every trim is "this User's rows, newest first".
CREATE INDEX ix_scim_user_password_history_user
    ON scim_user_password_history (user_id, set_at DESC);

GRANT SELECT, INSERT, DELETE ON scim_user_password_history TO backend_app;
