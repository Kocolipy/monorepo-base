-- A bulk read — a query of the User or Group collection, or a base search across
-- both — is audited as one event carrying what it returned and the SHAPE of its
-- filter, so a full-directory sync or a zero-result probe by a compromised token is
-- distinguishable from ordinary traffic in the trail.
--
-- Two nullable columns rather than a side table: every other operation leaves them
-- NULL, and nothing queries them apart from the event they belong to.

-- How many resources the response carried. Not totalResults: what a token was HANDED
-- is the exposure, and a count=0 probe learns a total without receiving anyone.
ALTER TABLE audit_events
    ADD COLUMN result_count INTEGER;

-- The filter with every literal replaced by `?`: canonical attribute paths, operators
-- and logical structure only, e.g. `(userName eq ? and emails[type eq ?])`. Rendered
-- by the application from a closed vocabulary; a filter's VALUES are exactly what the
-- trail must not become a copy of. Bounded in practice by the filter's own node limit
-- (100 expressions).
ALTER TABLE audit_events
    ADD COLUMN filter_shape TEXT;

ALTER TABLE audit_events
    ADD CONSTRAINT ck_audit_events_result_count_non_negative
        CHECK (result_count IS NULL OR result_count >= 0);
