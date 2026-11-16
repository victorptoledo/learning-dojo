-- Some tests switch to the role app_api (created by your security.sql) to see what the API
-- sees. Your files are always read first, as the test user; the switch happens just before the
-- check. app_api needs to call the dojo's test helpers:
GRANT USAGE ON SCHEMA dojo TO PUBLIC;

-- 2,208 events, one per hour from October to December 2026, for tenants 1, 2 and 3 in turn,
-- and one event of tenant 1 in February 2027.
CREATE FUNCTION pg_temp.load() RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO events (tenant_id, at, kind)
    SELECT 1 + i % 3, TIMESTAMPTZ '2026-10-01 00:00+00' + i * INTERVAL '1 hour',
           (ARRAY['login', 'purchase', 'error'])[1 + i % 5 % 3]
    FROM generate_series(0, 2207) AS i;
    INSERT INTO events (tenant_id, at, kind) VALUES (1, '2027-02-01 10:00+00', 'login');
END
$$;

-- Acts as the API serving a tenant (NULL: no tenant set).
CREATE FUNCTION pg_temp.as_api(tenant integer) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    SET LOCAL ROLE app_api;
    IF tenant IS NOT NULL THEN
        PERFORM set_config('app.tenant_id', tenant::text, true);
    END IF;
END
$$;

-- A learner's query as a query string, read now; a file still to write fails at check time.
CREATE FUNCTION pg_temp.fail(message text) RETURNS integer LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '%', message;
END
$$;
CREATE FUNCTION pg_temp.query_of(file text) RETURNS text LANGUAGE plpgsql AS $$
BEGIN
    RETURN 'SELECT * FROM (' || dojo.sql_of(file) || ') AS learner';
EXCEPTION WHEN OTHERS THEN
    RETURN format('SELECT pg_temp.fail(%L)', SQLERRM);
END
$$;

-- schema.sql
SELECT dojo.script_eq(
    'creates the monthly partitions and a default one',
    'schema.sql',
    $$SELECT c.relname, pg_get_expr(c.relpartbound, c.oid)
      FROM pg_inherits AS i JOIN pg_class AS c ON c.oid = i.inhrelid
      WHERE i.inhparent = 'events'::regclass$$,
    $$VALUES ('events_2026_10', 'FOR VALUES FROM (''2026-10-01 00:00:00+00'') TO (''2026-11-01 00:00:00+00'')'),
             ('events_2026_11', 'FOR VALUES FROM (''2026-11-01 00:00:00+00'') TO (''2026-12-01 00:00:00+00'')'),
             ('events_2026_12', 'FOR VALUES FROM (''2026-12-01 00:00:00+00'') TO (''2027-01-01 00:00:00+00'')'),
             ('events_default', 'DEFAULT')$$
);

SELECT dojo.script_eq(
    'routes every event to its month',
    'schema.sql',
    $$SELECT tableoid::regclass::text, count(*) FROM events GROUP BY 1$$,
    $$VALUES ('events_2026_10', 744), ('events_2026_11', 720), ('events_2026_12', 744), ('events_default', 1)$$,
    after => 'SELECT pg_temp.load()'
);

SELECT dojo.script_fails(
    'events belong to a known tenant',
    'schema.sql',
    $$INSERT INTO events (tenant_id, at, kind) VALUES (99, '2026-11-02 10:00+00', 'login')$$,
    '23503',
    'an event of tenant 99, which does not exist, must be rejected'
);

SELECT dojo.script_ok(
    'every partition has an index on tenant and time',
    'schema.sql',
    $$SELECT count(*) = 4 FROM pg_index AS i JOIN pg_inherits AS h ON h.inhrelid = i.indrelid
      WHERE h.inhparent = 'events'::regclass
        AND (SELECT array_agg(a.attname::text ORDER BY k.ord)
             FROM unnest(i.indkey::int2[]) WITH ORDINALITY AS k(attnum, ord)
             JOIN pg_attribute AS a ON a.attrelid = i.indrelid AND a.attnum = k.attnum) = '{tenant_id,at}'$$,
    'create one index on (tenant_id, at) on the parent table: every partition gets it'
);

SELECT dojo.script_ok(
    'a query on one month reads one partition',
    'schema.sql',
    $$SELECT dojo.plan($q$SELECT count(*) FROM events WHERE at >= '2026-11-05 00:00+00' AND at < '2026-11-06 00:00+00'$q$)
             !~ '(events_2026_10|events_2026_12|events_default)'$$,
    'the planner did not prune: is events partitioned by range of at?',
    after => 'SELECT pg_temp.load(); ANALYZE events'
);

-- security.sql (after schema.sql and the data)
SELECT dojo.script_eq(
    'a tenant sees only its own events',
    NULL,
    $$SELECT tenant_id, count(*) FROM events GROUP BY 1$$,
    $$VALUES (2, 736)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql')$$,
    after => 'SELECT pg_temp.as_api(2)'
);

SELECT dojo.script_eq(
    'without a tenant the API sees nothing',
    NULL,
    $$SELECT count(*) FROM events$$,
    $$VALUES (0)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql')$$,
    after => 'SELECT pg_temp.as_api(NULL)'
);

SELECT dojo.script_eq(
    'a tenant that was set and cleared means no tenant',
    NULL,
    $$SELECT count(*) FROM events$$,
    $$VALUES (0)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql');
                SELECT set_config('app.tenant_id', '', true)$$,
    after => 'SELECT pg_temp.as_api(NULL)'
);

SELECT dojo.script_fails(
    'the API cannot write events for another tenant',
    NULL,
    $$INSERT INTO events (tenant_id, at, kind) VALUES (3, '2026-11-02 10:00+00', 'login')$$,
    '42501',
    'tenant 1 inserted an event of tenant 3: the policy needs a WITH CHECK',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => 'SELECT pg_temp.as_api(1)'
);

SELECT dojo.script_eq(
    'the API can write its own events',
    NULL,
    $$SELECT tenant_id, kind FROM events$$,
    $$VALUES (1, 'login')$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => $$SELECT pg_temp.as_api(1); INSERT INTO events (tenant_id, at, kind) VALUES (1, '2026-11-02 10:00+00', 'login')$$
);

SELECT dojo.script_fails(
    'the API cannot delete events',
    NULL,
    $$DELETE FROM events$$,
    '42501',
    'app_api may read and add events, never delete them',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT dojo.run_file('security.sql')$$,
    after => 'SELECT pg_temp.as_api(1)'
);

SELECT dojo.script_ok(
    'the policy applies to the table owner too',
    'security.sql',
    $$SELECT relrowsecurity AND relforcerowsecurity FROM pg_class WHERE oid = 'events'::regclass$$,
    'enable row level security on events and force it for the owner',
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

-- retention.sql (after schema.sql and the data)
SELECT dojo.script_eq(
    'drops October and adds January',
    'retention.sql',
    $$SELECT tableoid::regclass::text, count(*) FROM events GROUP BY 1$$,
    $$VALUES ('events_2026_11', 720), ('events_2026_12', 744), ('events_2027_01', 1), ('events_default', 1)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load()$$,
    after => $$INSERT INTO events (tenant_id, at, kind) VALUES (2, '2027-01-15 08:00+00', 'login')$$
);

SELECT dojo.script_ok(
    'October is gone from the database, not only from events',
    'retention.sql',
    $$SELECT to_regclass('events_2026_10') IS NULL$$,
    'events_2026_10 still exists as a table: drop it after detaching it',
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load()$$
);

-- tenant_report.sql, run as the API
SELECT dojo.script_eq(
    'reports the current tenant''s November events by kind',
    NULL,
    pg_temp.query_of('tenant_report.sql'),
    $$VALUES ('error', 48), ('login', 96), ('purchase', 96)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql')$$,
    after => 'SELECT pg_temp.as_api(3)'
);

SELECT dojo.script_eq(
    'relies on the policy for the tenant',
    NULL,
    pg_temp.query_of('tenant_report.sql'),
    $$VALUES ('error', 48), ('login', 97), ('purchase', 96)$$,
    setup => $$SELECT dojo.run_file('schema.sql'); SELECT pg_temp.load(); SELECT dojo.run_file('security.sql');
                INSERT INTO events (tenant_id, at, kind) VALUES (1, '2026-11-30 23:59+00', 'login'),
                                                               (1, '2026-12-01 00:00+00', 'login')$$,
    after => 'SELECT pg_temp.as_api(1)'
);

SELECT dojo.script_ok(
    'the report reads only the November partition',
    'schema.sql',
    $$SELECT dojo.plan(dojo.sql_of('tenant_report.sql')) !~ '(events_2026_10|events_2026_12|events_default)'$$,
    'tenant_report.sql reads other months: filter on at with a range the planner can prune',
    after => 'SELECT pg_temp.load(); ANALYZE events'
);
