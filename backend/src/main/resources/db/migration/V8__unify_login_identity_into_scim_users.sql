-- One identity, not two. The `accounts` table and the SCIM directory described the
-- same people in two places; this migration makes the SCIM User the only login
-- identity and removes the other.
--
-- The specification plan settled this as "SCIM User replaces Account and owns
-- profile plus authentication state", and what that costs concretely is four
-- columns moving: the credential and the standing flag have SCIM counterparts
-- already (`password_hash`, `active`), the failure run and the lock instant do not
-- and are added below, and the role column has no counterpart at all because
-- authority is now derived from Admin group membership.
--
-- ## Why the rows are dropped rather than copied
--
-- Copying each account into `scim_users` under its own id was tried first and
-- rejected, and the reason is the Bootstrap Admin. Which User is the deployment's
-- recovery identity is recorded in `scim_resources.reserved_name`, a column no
-- UPDATE may reach — that immutability is what makes the protection worth having,
-- since a marker a write could change is a protection a write could remove. But the
-- configured recovery username lives in application configuration, which a Flyway
-- migration cannot read. So a copied administrator would arrive UNRESERVED, and
-- startup seeding would then find a User already holding that userName, conclude
-- "it is already there", and leave a deployment whose recovery identity has neither
-- its write protection nor its exemption from lockout — silently, and precisely for
-- the identity where silence is most expensive.
--
-- Dropping the rows removes that failure mode instead of guarding against it. The
-- application is pre-production and the specification plan puts migration and
-- cutover out of scope, so there is no data here to preserve; startup seeding
-- recreates both configured identities from the same configuration, under the same
-- names and passwords, with the recovery identity marked as it is created.
--
-- What is genuinely lost is named rather than glossed: `user_counters` holds
-- showcase rows keyed by account id and they go with the accounts, and audit events
-- recorded against an account's id keep pointing at an id no resource carries any
-- more. The audit stream is append-only, so those rows are history that cannot be
-- rewritten and are not; they are pre-production history of a development database.

-- 1. The authentication state a SCIM User did not carry.
--
-- Both columns mean exactly what they meant on `accounts`, including the part V6
-- settled: `locked_at` records when a lock was IMPOSED and nothing about when it
-- ends, because it does not end on its own. A lock is lifted by an administrator's
-- Unlock, so the column's presence is the state and no clock is consulted to read
-- it.
ALTER TABLE scim_users
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;

-- The default is a genuine one, unlike `active`, whose default belongs to the create
-- use case where RFC 7643 puts it: zero is the only value a newly provisioned User's
-- failure run can start at, and a SCIM create names neither column.
ALTER TABLE scim_users
    ADD COLUMN locked_at TIMESTAMPTZ;

COMMENT ON COLUMN scim_users.locked_at IS
    'When the User was locked by its failure run; NULL when it is not locked. '
    'A lock has no expiry — only an administrator''s Unlock clears this.';

-- 2. `user_counters` follows the identity rather than the table it used to point at.
--
-- The rows go first: every one of them is keyed by an account id that is about to
-- stop existing, and there is no id to re-key them to — the Users that replace those
-- accounts are created by seeding, after this migration has run. V2 set the
-- precedent for exactly this, dropping counter rows it could not re-key rather than
-- carrying them under a key that means nothing.
DELETE FROM user_counters;

-- Renamed as well as repointed: the column is called `account_id` because an account
-- aggregate existed, and leaving the name behind after dropping the aggregate is how
-- a later reader concludes there are still two kinds of identity.
ALTER TABLE user_counters
    DROP CONSTRAINT fk_user_counters_account;

ALTER TABLE user_counters
    RENAME COLUMN account_id TO user_id;

ALTER TABLE user_counters
    ADD CONSTRAINT fk_user_counters_user
        FOREIGN KEY (user_id) REFERENCES scim_users (resource_id) ON DELETE CASCADE;

-- 3. The table, and with it the role column.
--
-- No `role` to drop separately: authority is derived from membership of the Admin
-- group, so the column has no successor and nothing reads it. Dropping the whole
-- table is what makes "the legacy role field is removed" checkable — a column left
-- behind unread is indistinguishable from one a future edit starts reading again.
DROP TABLE accounts;
