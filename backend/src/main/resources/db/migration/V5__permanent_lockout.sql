-- A lockout no longer has a duration: it is imposed by the failure run and lifted
-- only by an administrator's Unlock. The column therefore stops recording when a
-- lock would end and starts recording when it was imposed, which is the one fact
-- the new rule needs and the only one that cannot be derived from anything else.
--
-- A straight rename would mean something different for the two kinds of row this
-- column can hold, so both are settled here rather than left to the application
-- to interpret:
--
--   * a row whose window had already run out was NOT locked under the old rule.
--     Renaming it in place would silently impose a permanent lock on an account
--     that was free to log in, so the value is dropped.
--   * a row still inside its window WAS locked, and stays locked — permanently
--     now, until an Unlock. The instant it was imposed is not recorded anywhere,
--     so the migration time stands in for it: the field means "locked since", and
--     nothing in the rule reads it to decide anything.

ALTER TABLE accounts
    RENAME COLUMN locked_until TO locked_at;

UPDATE accounts
   SET locked_at = CASE WHEN locked_at > now() THEN now() ELSE NULL END
 WHERE locked_at IS NOT NULL;

COMMENT ON COLUMN accounts.locked_at IS
    'When the account was locked by its failure run; NULL when it is not locked. '
    'A lock has no expiry — only an administrator''s Unlock clears this.';
