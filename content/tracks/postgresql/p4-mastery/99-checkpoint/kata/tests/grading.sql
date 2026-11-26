-- slots.sql
SELECT dojo.query_eq(
    'a slot just over 1 GB is listed',
    'slots.sql',
    $$VALUES ('debezium', 'logical', false, 2304), ('standby_c', 'physical', true, 1024),
             ('standby_b', 'physical', true, 1344), ('old_reports', 'physical', false, 24),
             ('lost_one', 'physical', false, NULL)$$,
    setup => $$UPDATE slots_snapshot SET restart_lsn = '5/3FFFFFFF' WHERE slot_name = 'standby_c'$$
);

SELECT dojo.query_eq(
    'a slot about to lose its WAL is listed',
    'slots.sql',
    $$VALUES ('debezium', 'logical', false, 2304), ('standby_b', 'physical', true, 1344),
             ('old_reports', 'physical', false, 24), ('standby_a', 'physical', true, 16),
             ('lost_one', 'physical', false, NULL)$$,
    ordered => true,
    setup => $$UPDATE slots_snapshot SET wal_status = 'unreserved' WHERE slot_name = 'standby_a'$$
);

SELECT dojo.query_eq(
    'measures from the primary''s position of the snapshot',
    'slots.sql',
    $$VALUES ('debezium', 'logical', false, 2560), ('standby_b', 'physical', true, 1600),
             ('etl', 'logical', true, 1264), ('standby_c', 'physical', true, 1280),
             ('old_reports', 'physical', false, 280), ('lost_one', 'physical', false, NULL)$$,
    setup => $$UPDATE primary_status SET current_lsn = '5/90000000'$$
);

-- restore_plan.sql
SELECT dojo.query_eq(
    'plans the restore to 14:29',
    'restore_plan.sql',
    $$VALUES ('full-20261125-0100', '0000000100000002000000CE', '00000001000000030000001E', 81, 1)$$
);

SELECT dojo.query_eq(
    'a complete archive has nothing missing',
    'restore_plan.sql',
    $$VALUES ('full-20261125-0100', '0000000100000002000000CE', '00000001000000030000001E', 81, 0)$$,
    setup => $$INSERT INTO wal_archive VALUES ('0000000100000002000000F0', '2026-11-25 13:50+00')$$
);

SELECT dojo.query_eq(
    'falls back to an older backup across the FF boundary',
    'restore_plan.sql',
    $$VALUES ('full-20261124-0100', '00000001000000020000003E', '00000001000000030000001E', 225, 2)$$,
    setup => $$DELETE FROM base_backups WHERE label = 'full-20261125-0100'$$
);

SELECT dojo.query_eq(
    'a successful backup just before the target wins',
    'restore_plan.sql',
    $$VALUES ('full-20261125-1400', '00000001000000030000001C', '00000001000000030000001E', 3, 0)$$,
    setup => $$UPDATE base_backups SET finished_at = '2026-11-25 14:28+00', stop_lsn = '3/1D000064'
                WHERE label = 'full-20261125-1400'$$
);

-- recover.sql
SELECT dojo.script_eq(
    'puts back every deleted payment and refund',
    'recover.sql',
    $$SELECT (SELECT count(*) FROM payments), (SELECT count(*) FROM refunds)$$,
    $$VALUES (1210, 120)$$
);

SELECT dojo.script_eq(
    'restored rows keep their ids and values',
    'recover.sql',
    $$SELECT p.id, p.customer_id, p.amount, p.status, p.created_at, r.id, r.amount
      FROM payments AS p JOIN refunds AS r ON r.payment_id = p.id WHERE p.id IN (50, 640) ORDER BY p.id$$,
    $$SELECT p.id, p.customer_id, p.amount, p.status, p.created_at, r.id, r.amount
      FROM restored.payments AS p JOIN restored.refunds AS r ON r.payment_id = p.id WHERE p.id IN (50, 640) ORDER BY p.id$$,
    ordered => true
);

SELECT dojo.script_eq(
    'changes made after 14:29 survive',
    'recover.sql',
    $$SELECT id, status FROM payments WHERE id IN (700, 1205)
      UNION ALL SELECT -id, payment_id::text FROM refunds WHERE id IN (110, 121)$$,
    $$VALUES (700, 'paid'), (1205, 'paid'), (-121, '1205')$$
);

SELECT dojo.script_eq(
    'running it twice changes nothing',
    'recover.sql',
    $$SELECT (SELECT count(*) FROM payments), (SELECT count(*) FROM refunds),
             (SELECT sum(amount) FROM payments), (SELECT sum(amount) FROM refunds)$$,
    $$SELECT 1210::bigint, 120::bigint,
             (SELECT sum(amount) FROM restored.payments) + 255 - 0,
             (SELECT sum(amount) FROM restored.refunds) + 12.50 - (SELECT amount FROM restored.refunds WHERE id = 110)$$,
    after => $$SELECT dojo.run_file('recover.sql')$$
);

SELECT dojo.script_eq(
    'new payments still get fresh ids',
    'recover.sql',
    $$SELECT id FROM payments WHERE created_at = '2026-11-26 09:00+00'$$,
    $$VALUES (1211)$$,
    after => $$INSERT INTO payments (customer_id, amount, status, created_at) VALUES (1, 10, 'paid', '2026-11-26 09:00+00')$$
);

-- wal_budget.sql
SELECT dojo.script_ok(
    'measures the WAL of a statement and runs it',
    'wal_budget.sql',
    $$SELECT (SELECT bytes FROM measured) > 1000000 AND (SELECT count(*) FROM ledger) = 20000$$,
    '20,000 rows of ledger write well over 1 MB of WAL and must all be inserted: is the position read before and after running the statement?',
    after => $$CREATE TEMP TABLE measured AS
               SELECT wal_bytes($q$INSERT INTO ledger (account, amount, at)
                                   SELECT i % 100, i, '2026-11-25' FROM generate_series(1, 20000) AS i$q$) AS bytes$$
);

SELECT dojo.script_ok(
    'a read writes no WAL',
    'wal_budget.sql',
    $$SELECT wal_bytes('SELECT count(*) FROM payments') < 8192$$,
    'wal_bytes of a SELECT must be (nearly) zero'
);

SELECT dojo.script_ok(
    'counts WAL still in the buffers',
    'wal_budget.sql',
    $$SELECT wal_bytes($q$INSERT INTO ledger (account, amount, at) VALUES (1, 1, '2026-11-25')$q$) BETWEEN 1 AND 8192$$,
    'one small insert writes a few hundred bytes of WAL that are not written out yet: read the insert position'
);

SELECT dojo.script_ok(
    'the staging table is unlogged',
    'wal_budget.sql',
    $$SELECT relpersistence = 'u' FROM pg_class WHERE oid = 'import_staging'::regclass$$,
    'import_staging must be UNLOGGED'
);

SELECT dojo.script_ok(
    'loading the staging table writes almost no WAL',
    'wal_budget.sql',
    $$SELECT wal_bytes($q$INSERT INTO import_staging SELECT i, repeat('x', 80) FROM generate_series(1, 20000) AS i$q$) < 100000$$,
    'loading 20,000 raw lines wrote too much WAL'
);
