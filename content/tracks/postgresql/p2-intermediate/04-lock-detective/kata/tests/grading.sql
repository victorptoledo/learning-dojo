-- Other sessions, opened with dblink, play the rest of the application.
CREATE EXTENSION IF NOT EXISTS dblink;

-- Opens a session called app (also its application_name) and runs sql in it, without waiting
-- for the result when async is true.
CREATE FUNCTION pg_temp.session(app text, sql text, async boolean DEFAULT false) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT app = ANY (coalesce(dblink_get_connections(), '{}')) THEN
        PERFORM dblink_connect(app, format('host=/tmp dbname=dojo user=dojo application_name=%s', app));
    END IF;
    IF async THEN
        PERFORM dblink_send_query(app, sql);
    ELSE
        PERFORM dblink_exec(app, sql);
    END IF;
END
$$;

-- Waits until the session app is waiting for a lock (at most 5 s).
CREATE FUNCTION pg_temp.wait_for_lock(app text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    FOR i IN 1 .. 100 LOOP
        PERFORM pg_stat_clear_snapshot();  -- pg_stat_activity is read once per transaction otherwise
        IF EXISTS (SELECT 1 FROM pg_stat_activity WHERE application_name = app AND wait_event_type = 'Lock') THEN
            RETURN;
        END IF;
        PERFORM pg_sleep(0.05);
    END LOOP;
    RAISE EXCEPTION 'test setup: % never waited for a lock', app;
END
$$;

-- Waits until no session called app is left (at most 5 s), and says whether it is gone.
CREATE FUNCTION pg_temp.gone(app text) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
    FOR i IN 1 .. 100 LOOP
        PERFORM pg_stat_clear_snapshot();  -- pg_stat_activity is read once per transaction otherwise
        IF NOT EXISTS (SELECT 1 FROM pg_stat_activity WHERE application_name = app) THEN
            RETURN true;
        END IF;
        PERFORM pg_sleep(0.05);
    END LOOP;
    RETURN false;
END
$$;

-- Ends every other session, so the next test starts clean.
CREATE FUNCTION pg_temp.close_sessions() RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    name text;
BEGIN
    FOREACH name IN ARRAY coalesce(dblink_get_connections(), '{}') LOOP
        PERFORM dblink_disconnect(name);
    END LOOP;
    FOR i IN 1 .. 100 LOOP
        PERFORM pg_stat_clear_snapshot();  -- pg_stat_activity is read once per transaction otherwise
        EXIT WHEN NOT EXISTS (SELECT 1 FROM pg_stat_activity
                              WHERE pid <> pg_backend_pid() AND backend_type = 'client backend');
        PERFORM pg_sleep(0.05);
    END LOOP;
END
$$;

-- billing changed account 1 and went quiet; checkout wants the same row; the migration wants
-- the whole table; the report queues behind the migration.
CREATE FUNCTION pg_temp.incident(with_migration boolean DEFAULT true) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    -- Every session works inside a transaction, so nothing it does is ever committed: closing
    -- the sessions rolls it all back, even after the blocker is terminated.
    PERFORM pg_temp.session('billing', 'BEGIN');
    PERFORM pg_temp.session('billing', $q$UPDATE accounts SET balance = balance - 10 WHERE id = 1$q$);
    PERFORM pg_temp.session('checkout', 'BEGIN');
    PERFORM pg_temp.session('checkout', $q$UPDATE accounts SET balance = balance + 5 WHERE id = 1$q$, async => true);
    PERFORM pg_temp.wait_for_lock('checkout');
    IF with_migration THEN
        PERFORM pg_temp.session('migration', 'BEGIN');
        PERFORM pg_temp.session('migration', $q$ALTER TABLE accounts ADD COLUMN currency char(3)$q$, async => true);
        PERFORM pg_temp.wait_for_lock('migration');
        PERFORM pg_temp.session('report', $q$SELECT count(*) FROM accounts$q$, async => true);
        PERFORM pg_temp.wait_for_lock('report');
    END IF;
END
$$;

-- blockers.sql
SELECT pg_temp.incident();
SELECT dojo.query_eq(
    'finds the whole blocking chain',
    'blockers.sql',
    $$VALUES ('checkout', 'billing', 'idle in transaction', 'UPDATE accounts SET balance = balance - 10 WHERE id = 1'),
             ('migration', 'billing', 'idle in transaction', 'UPDATE accounts SET balance = balance - 10 WHERE id = 1'),
             ('migration', 'checkout', 'active', 'UPDATE accounts SET balance = balance + 5 WHERE id = 1'),
             ('report', 'migration', 'active', 'ALTER TABLE accounts ADD COLUMN currency char(3)')$$,
    ordered => true
);
SELECT pg_temp.close_sessions();

SELECT pg_temp.incident(with_migration => false);
SELECT pg_temp.session('idle', 'SET search_path = public');
SELECT dojo.query_eq(
    'lists only sessions that wait',
    'blockers.sql',
    $$VALUES ('checkout', 'billing', 'idle in transaction', 'UPDATE accounts SET balance = balance - 10 WHERE id = 1')$$,
    ordered => true
);
SELECT pg_temp.close_sessions();

SELECT dojo.query_eq(
    'returns nothing when nobody waits',
    'blockers.sql',
    $$SELECT 'a', 'b', 'c', 'd' WHERE false$$
);

-- root_blockers.sql: each test starts the incident again, because terminating is real.
SELECT pg_temp.incident();
SELECT dojo.query_eq(
    'ends only the root of the chain',
    'root_blockers.sql',
    $$VALUES ('billing', true)$$
);
SELECT dojo.script_ok(
    'the root blocker is gone and the others are still there',
    NULL,
    $$SELECT pg_temp.gone('billing')
         AND EXISTS (SELECT 1 FROM pg_stat_activity WHERE application_name = 'checkout')
         AND EXISTS (SELECT 1 FROM pg_stat_activity WHERE application_name = 'report')$$,
    'billing should be terminated and checkout and report left alone'
);
SELECT pg_temp.close_sessions();

SELECT pg_temp.session('admin', 'BEGIN');
SELECT pg_temp.session('admin', 'LOCK TABLE accounts IN SHARE MODE');
SELECT pg_temp.session('writer', $q$UPDATE accounts SET balance = 0 WHERE id = 2$q$, async => true);
SELECT pg_temp.wait_for_lock('writer');
SELECT pg_temp.session('bystander', 'SET search_path = public');
SELECT dojo.query_eq(
    'ends any root blocker, and nobody who blocks no one',
    'root_blockers.sql',
    $$VALUES ('admin', true)$$
);
SELECT pg_temp.close_sessions();

-- migration.sql
SELECT pg_temp.incident(with_migration => false);
SELECT dojo.script_fails(
    'gives up quickly when the table is locked',
    NULL,
    $$SELECT dojo.run_file('migration.sql')$$,
    '55P03',
    'with billing holding a lock, the migration must fail with lock_timeout (55P03) instead of waiting'
);
SELECT pg_temp.close_sessions();

SELECT dojo.script_ok(
    'waits at most 2 seconds for a lock',
    'migration.sql',
    $$SELECT current_setting('lock_timeout') = '2s'$$,
    'set lock_timeout to 2s for the migration'
);

SELECT dojo.script_fails(
    'new rows must have a positive balance',
    'migration.sql',
    $$INSERT INTO accounts (id, owner, balance) VALUES (5000, 'x', -1)$$,
    '23514',
    'the balance check must apply to new rows'
);

SELECT dojo.script_eq(
    'new rows get EUR by default',
    'migration.sql',
    $$SELECT currency FROM accounts WHERE id = 5000$$,
    $$VALUES ('EUR')$$,
    after => $$INSERT INTO accounts (id, owner, balance) VALUES (5000, 'x', 1)$$
);

-- singleton.sql
SELECT dojo.query_eq(
    'takes the lock when nobody holds it',
    'singleton.sql',
    $$VALUES (true)$$
);

SELECT dojo.script_ok(
    'releases the lock at the end of the transaction',
    NULL,
    $$SELECT NOT EXISTS (SELECT 1 FROM pg_locks WHERE locktype = 'advisory' AND pid = pg_backend_pid())$$,
    'the advisory lock outlived the transaction: use a transaction-level lock',
    setup => $$SELECT dojo.sql_of('singleton.sql')$$   -- only once singleton.sql is written
);

SELECT pg_temp.session('other-instance', 'DO $$BEGIN PERFORM pg_advisory_lock(4242); END$$');
SELECT dojo.query_eq(
    'does not wait when another instance holds the lock',
    'singleton.sql',
    $$VALUES (false)$$
);
SELECT pg_temp.close_sessions();
