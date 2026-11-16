-- sessions.sql: a schema test, then a load test. The HOT counters come from
-- pg_stat_xact_user_tables, which counts what the current transaction did.
CREATE FUNCTION pg_temp.load_sessions() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO sessions (id, user_id, token, last_seen, created_at)
    SELECT i, i % 700, md5(i::text), TIMESTAMPTZ '2026-11-15 12:00+00', TIMESTAMPTZ '2026-11-01 00:00+00'
    FROM generate_series(1, 5000) AS i;
END
$$;

SELECT dojo.script_eq(
    'sessions has the five columns',
    'sessions.sql',
    $$SELECT attname, format_type(atttypid, atttypmod), attnotnull FROM pg_attribute
      WHERE attrelid = 'sessions'::regclass AND attnum > 0 AND NOT attisdropped ORDER BY attnum$$,
    $$VALUES ('id', 'bigint', true), ('user_id', 'bigint', true), ('token', 'text', true),
             ('last_seen', 'timestamp with time zone', true), ('created_at', 'timestamp with time zone', true)$$,
    ordered => true
);

SELECT dojo.script_fails(
    'a token belongs to one session only',
    'sessions.sql',
    $$INSERT INTO sessions VALUES (1, 1, 'abc', now(), now()), (2, 2, 'abc', now(), now())$$,
    '23505',
    'two sessions with the same token must be rejected'
);

SELECT dojo.script_ok(
    'finds the sessions of a user through an index',
    'sessions.sql',
    $$SELECT dojo.plan('SELECT * FROM sessions WHERE user_id = 42') !~ 'Seq Scan'$$,
    'SELECT * FROM sessions WHERE user_id = 42 reads the whole table: index user_id',
    after => $$SELECT pg_temp.load_sessions(); ANALYZE sessions$$
);

SELECT dojo.script_ok(
    'finds a session by its token through an index',
    'sessions.sql',
    $$SELECT dojo.plan($q$SELECT * FROM sessions WHERE token = 'c4ca4238a0b923820dcc509a6f75849b'$q$) !~ 'Seq Scan'$$,
    'a lookup by token reads the whole table',
    after => $$SELECT pg_temp.load_sessions(); ANALYZE sessions$$
);

SELECT dojo.script_ok(
    'updates of last_seen are HOT',
    'sessions.sql',
    $$SELECT n_tup_upd = 250 AND n_tup_hot_upd >= 225 FROM pg_stat_xact_user_tables WHERE relname = 'sessions'$$,
    'fewer than 90% of the 250 updates of last_seen were HOT: is last_seen in an index, and is there free space on the pages (fillfactor)?',
    after => $$SELECT pg_temp.load_sessions();
                UPDATE sessions SET last_seen = last_seen + INTERVAL '1 minute' WHERE id % 20 = 0$$
);

SELECT dojo.script_ok(
    'keeps most of each page for rows',
    'sessions.sql',
    $$SELECT coalesce((SELECT split_part(o, '=', 2)::int FROM unnest(reloptions) AS o
                       WHERE o LIKE 'fillfactor=%'), 100) BETWEEN 50 AND 95
      FROM pg_class WHERE oid = 'sessions'::regclass$$,
    'the fillfactor of sessions should leave some room (at most 95) but not waste half the table (at least 50)'
);

-- vacuum_due.sql
SELECT dojo.query_eq(
    'a per-table scale factor is used',
    'vacuum_due.sql',
    $$VALUES ('countries', 120, 100, false),
             ('events', 40000000, 25000050, false),
             ('orders', 450000, 400050, true),
             ('sessions', 30000, 15000, false),
             ('users', 9000, 5050, false)$$,
    ordered => true,
    setup => $$UPDATE stats_snapshot SET reloptions = '{autovacuum_vacuum_scale_factor=0.1}' WHERE relname = 'users'$$
);

SELECT dojo.query_eq(
    'a per-table threshold is used, and other options are ignored',
    'vacuum_due.sql',
    $$VALUES ('countries', 120, 100, false),
             ('events', 40000000, 25000050, false),
             ('orders', 450000, 400050, true),
             ('sessions', 30000, 15000, false)$$,
    ordered => true,
    setup => $$UPDATE stats_snapshot SET reloptions = '{fillfactor=70,autovacuum_vacuum_threshold=2000}' WHERE relname = 'carts'$$
);

SELECT dojo.query_eq(
    'reaching the trigger exactly is not enough',
    'vacuum_due.sql',
    $$VALUES ('carts', 251, 250, false),
             ('countries', 120, 100, false),
             ('events', 40000000, 25000050, false),
             ('orders', 450000, 400050, true),
             ('sessions', 30000, 15000, false)$$,
    ordered => true,
    setup => $$UPDATE stats_snapshot SET n_dead_tup = 251 WHERE relname = 'carts'$$
);

SELECT dojo.query_eq(
    'autovacuum_enabled=true is not disabled',
    'vacuum_due.sql',
    $$VALUES ('countries', 120, 100, false),
             ('events', 40000000, 25000050, false),
             ('orders', 450000, 400050, false),
             ('sessions', 30000, 15000, false)$$,
    ordered => true,
    setup => $$UPDATE stats_snapshot SET reloptions = '{fillfactor=90,autovacuum_enabled=true}' WHERE relname = 'orders'$$
);

-- tune.sql
SELECT dojo.script_eq(
    'sets the autovacuum parameters of events',
    'tune.sql',
    $$SELECT split_part(o, '=', 1), split_part(o, '=', 2)::float8
      FROM pg_class, unnest(reloptions) AS o WHERE oid = 'events'::regclass$$,
    $$VALUES ('autovacuum_vacuum_scale_factor', 0.01), ('autovacuum_vacuum_threshold', 1000),
             ('autovacuum_analyze_scale_factor', 0.005), ('autovacuum_vacuum_cost_limit', 1000)$$
);

SELECT dojo.script_eq(
    'gives orders its autovacuum back and keeps its fillfactor',
    'tune.sql',
    $$SELECT reloptions FROM pg_class WHERE oid = 'orders'::regclass$$,
    $$VALUES ('{fillfactor=90}'::text[])$$
);

-- wraparound.sql
SELECT dojo.query_eq(
    'reports every database by xid age',
    'wraparound.sql',
    $$SELECT datname, age(datfrozenxid),
             round(100.0 * age(datfrozenxid) / current_setting('autovacuum_freeze_max_age')::bigint, 1)
      FROM pg_database ORDER BY 2 DESC, 1$$,
    ordered => true
);
