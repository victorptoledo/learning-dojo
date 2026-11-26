-- A payments database, the day after an accident. The tests load this file into a fresh
-- PostgreSQL 17.

-- ---- 1. Replication slots, as the primary reported them at 14:00 -------------------------
CREATE TABLE primary_status (
    captured_at timestamptz NOT NULL,
    current_lsn pg_lsn NOT NULL
);
INSERT INTO primary_status VALUES ('2026-11-25 14:00+00', '5/80000000');

-- Copied from pg_replication_slots at the same moment.
CREATE TABLE slots_snapshot (
    slot_name   text PRIMARY KEY,
    slot_type   text NOT NULL,          -- physical | logical
    active      boolean NOT NULL,
    restart_lsn pg_lsn,                 -- NULL once the slot has lost its WAL
    wal_status  text NOT NULL           -- reserved | extended | unreserved | lost
);
INSERT INTO slots_snapshot VALUES
    ('standby_a',   'physical', true,  '5/7F000000', 'reserved'),
    ('standby_b',   'physical', true,  '5/2C000000', 'extended'),
    ('standby_c',   'physical', true,  '5/40000001', 'reserved'),
    ('etl',         'logical',  true,  '5/41000000', 'reserved'),
    ('debezium',    'logical',  false, '4/F0000000', 'extended'),
    ('old_reports', 'physical', false, '5/7E800000', 'reserved'),
    ('lost_one',    'physical', false, NULL,         'lost');

-- ---- 2. Base backups and the WAL archive, from the backup tool's catalog -----------------
-- Timeline 1, 16 MB segments. The server wrote about one segment every 10 minutes.
CREATE TABLE base_backups (
    label       text PRIMARY KEY,
    started_at  timestamptz NOT NULL,
    finished_at timestamptz,            -- NULL: the backup failed
    start_lsn   pg_lsn NOT NULL,
    stop_lsn    pg_lsn
);

CREATE TABLE wal_archive (
    segment     text PRIMARY KEY,       -- the WAL file name
    archived_at timestamptz NOT NULL
);

-- Segment number 568 + k holds the WAL written from 2026-11-24 00:00 + 10k minutes on, and was
-- archived when it filled. A lifecycle rule on the bucket deleted two files by mistake.
INSERT INTO wal_archive (segment, archived_at)
SELECT '00000001' || lpad(upper(to_hex(s / 256)), 8, '0') || lpad(upper(to_hex(s % 256)), 8, '0'),
       TIMESTAMPTZ '2026-11-24 00:00+00' + (s - 568 + 1) * INTERVAL '10 minutes'
FROM generate_series(568, 808) AS s
WHERE s NOT IN (608, 752);

CREATE FUNCTION pg_temp.lsn_at(k integer, offset_bytes integer) RETURNS pg_lsn
LANGUAGE sql IMMUTABLE AS $$ SELECT '0/0'::pg_lsn + ((568 + k)::numeric * 16777216 + offset_bytes) $$;

INSERT INTO base_backups VALUES
    ('full-20261124-0100', '2026-11-24 01:00+00', '2026-11-24 01:40+00', pg_temp.lsn_at(6, 40), pg_temp.lsn_at(10, 5000)),
    ('full-20261125-0100', '2026-11-25 01:00+00', '2026-11-25 01:35+00', pg_temp.lsn_at(150, 40), pg_temp.lsn_at(153, 9000)),
    ('full-20261125-1400', '2026-11-25 14:00+00', NULL, pg_temp.lsn_at(228, 40), NULL),
    ('full-20261125-1420', '2026-11-25 14:20+00', '2026-11-25 14:41+00', pg_temp.lsn_at(230, 40), pg_temp.lsn_at(232, 700));

-- ---- 3. The payments, after the accident --------------------------------------------------
CREATE TABLE customers (
    id   integer PRIMARY KEY,
    name text NOT NULL
);
INSERT INTO customers SELECT c, 'Customer ' || c FROM generate_series(1, 50) AS c;

CREATE TABLE payments (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers (id),
    amount      numeric(10, 2) NOT NULL,
    status      text NOT NULL,
    created_at  timestamptz NOT NULL
);

CREATE TABLE refunds (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id bigint NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    amount     numeric(10, 2) NOT NULL,
    created_at timestamptz NOT NULL
);

INSERT INTO payments (customer_id, amount, status, created_at)
SELECT 1 + i % 50, round((5 + i * 7.31 % 400)::numeric, 2),
       CASE WHEN i % 50 = 0 THEN 'pending' ELSE 'paid' END,
       TIMESTAMPTZ '2026-08-01 00:00+00' + i * INTERVAL '135 minutes'
FROM generate_series(1, 1200) AS i;

INSERT INTO refunds (payment_id, amount, created_at)
SELECT p.id, round(p.amount / 2, 2), p.created_at + INTERVAL '1 day'
FROM payments AS p
WHERE p.id % 10 = 0
ORDER BY p.id;

-- The point-in-time recovery to 14:29 ran on a spare server, and its two tables were copied
-- here into the schema restored.
CREATE SCHEMA restored;
CREATE TABLE restored.payments AS TABLE payments;
CREATE TABLE restored.refunds AS TABLE refunds;

-- 14:30: a clean-up meant for the archive database ran on the primary.
DELETE FROM payments WHERE created_at < '2026-10-01';

-- After 14:30 the shop kept working.
UPDATE payments SET status = 'paid' WHERE id = 700;
INSERT INTO payments (customer_id, amount, status, created_at)
SELECT 1 + i % 50, 20 + i, 'paid', TIMESTAMPTZ '2026-11-25 15:00+00' + i * INTERVAL '5 minutes'
FROM generate_series(1, 10) AS i;
INSERT INTO refunds (payment_id, amount, created_at) VALUES (1205, 12.50, '2026-11-25 16:00+00');
DELETE FROM refunds WHERE id = 110;      -- support removed a refund entered twice, at 15:10

-- ---- 4. The nightly import ----------------------------------------------------------------
CREATE TABLE ledger (
    id      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account integer NOT NULL,
    amount  numeric(12, 2) NOT NULL,
    at      timestamptz NOT NULL
);

-- Raw lines of the bank's file land here and are parsed into ledger; the file can always be
-- downloaded again.
CREATE TABLE import_staging (
    line integer NOT NULL,
    raw  text NOT NULL
);
