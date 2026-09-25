-- Adds the stable, non-reassignable identifier the account aggregate needs, and
-- re-keys every application-owned table that pointed at a username so it keeps
-- pointing at the same account through a later rename. Redis session indexing is
-- re-keyed the same way, but that is application-owned state outside Postgres
-- and has no row here to migrate: AuthController starts writing the new index
-- key the moment this version is live, and a session minted before that still
-- carries the old, username-keyed index entry until it is next renewed by a
-- fresh login.
--
-- pgcrypto's gen_random_uuid() backfills every existing row with a real,
-- distinct id in the same statement that adds the column, so there is never a
-- moment where an account exists without one.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE accounts
    ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE accounts
    DROP CONSTRAINT pk_accounts;

ALTER TABLE accounts
    ADD CONSTRAINT pk_accounts PRIMARY KEY (id);

ALTER TABLE accounts
    ADD CONSTRAINT uq_accounts_username UNIQUE (username);

-- The application always assigns an id itself before the first insert, so the
-- column keeps its NOT NULL constraint but drops the server-side default: a
-- future row with no id supplied is a defect in the application, not something
-- the database should paper over.
ALTER TABLE accounts
    ALTER COLUMN id DROP DEFAULT;

-- user_counters re-keyed from username to the account's new stable id. The
-- table is pre-production seed/showcase data only, so the existing rows are
-- carried across by joining on the username they were keyed by, rather than
-- preserved by any migration tooling that would matter with real user data.
ALTER TABLE user_counters
    ADD COLUMN account_id UUID;

UPDATE user_counters uc
   SET account_id = a.id
  FROM accounts a
 WHERE a.username = uc.username;

-- A counter for a username with no matching account cannot be re-keyed and is
-- orphaned data from a deleted or never-existing account; it is dropped rather
-- than carried forward under a null key.
DELETE FROM user_counters WHERE account_id IS NULL;

ALTER TABLE user_counters
    DROP CONSTRAINT pk_user_counters;

ALTER TABLE user_counters
    ALTER COLUMN account_id SET NOT NULL;

ALTER TABLE user_counters
    ADD CONSTRAINT pk_user_counters PRIMARY KEY (account_id);

ALTER TABLE user_counters
    ADD CONSTRAINT fk_user_counters_account
        FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE;

ALTER TABLE user_counters
    DROP COLUMN username;
