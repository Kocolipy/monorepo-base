-- Baseline: the schema exactly as Hibernate's ddl-auto had been generating it,
-- captured once under version control before ddl-auto is switched off. This
-- migration exists to give V2's stable-id addition something real to migrate;
-- the app is pre-production, so there is no data to preserve past this point,
-- only the shape that every previous run created.

CREATE TABLE accounts (
    username              VARCHAR(255) NOT NULL,
    password_hash         VARCHAR(255),
    role                  VARCHAR(255),
    failed_login_attempts INTEGER      NOT NULL,
    locked_until          TIMESTAMPTZ,
    enabled               BOOLEAN,
    created_at            TIMESTAMPTZ,
    CONSTRAINT pk_accounts PRIMARY KEY (username)
);

CREATE TABLE user_counters (
    username VARCHAR(255) NOT NULL,
    count    BIGINT       NOT NULL,
    CONSTRAINT pk_user_counters PRIMARY KEY (username)
);
