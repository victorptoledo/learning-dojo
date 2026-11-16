-- The vacuum clinic. The tests load this file into a fresh PostgreSQL 17.

-- Two live tables whose autovacuum settings you will fix.
CREATE TABLE orders (
    id        bigint PRIMARY KEY,
    status    text NOT NULL,
    placed_at timestamptz NOT NULL
) WITH (autovacuum_enabled = false, fillfactor = 90);   -- someone switched autovacuum off

CREATE TABLE events (
    id      bigint PRIMARY KEY,
    at      timestamptz NOT NULL,
    kind    text NOT NULL,
    payload jsonb
);

-- What the on-call engineer exported from production last night: pg_stat_user_tables joined
-- with pg_class (reltuples and reloptions), one row per table.
CREATE TABLE stats_snapshot (
    relname    text PRIMARY KEY,
    reltuples  bigint NOT NULL,
    n_dead_tup bigint NOT NULL,
    reloptions text[]
);

INSERT INTO stats_snapshot (relname, reltuples, n_dead_tup, reloptions) VALUES
    ('users',     50000,     9000,     NULL),
    ('orders',    2000000,   450000,   '{autovacuum_enabled=false,fillfactor=90}'),
    ('events',    500000000, 40000000, '{autovacuum_vacuum_scale_factor=0.05}'),
    ('sessions',  100000,    30000,    '{fillfactor=80,autovacuum_vacuum_threshold=5000,autovacuum_vacuum_scale_factor=0.1}'),
    ('audit_log', 10000000,  1500000,  '{autovacuum_vacuum_threshold=100000}'),
    ('countries', 250,       120,      NULL),
    ('carts',     1000,      250,      NULL);
