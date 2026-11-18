-- Helpers of the ledger tests (no tests here). Loaded before the stage files.
--
-- The learner's files are loaded once and committed (at the end of this file), because the
-- concurrency tests open real extra sessions with dblink, and those only see committed
-- objects. A failed load is remembered and reported by every test that needs the file.
-- The other tests run inside the usual rolled-back block.

CREATE EXTENSION dblink;
CREATE SCHEMA ledger_test;

CREATE TABLE ledger_test.load_errors (
    file    text PRIMARY KEY,
    message text NOT NULL
);

CREATE FUNCTION ledger_test.load(file text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE dojo.sql_of(file);
    EXCEPTION WHEN OTHERS THEN
        INSERT INTO ledger_test.load_errors
        VALUES (file, CASE WHEN SQLERRM LIKE 'todo:%' OR SQLERRM LIKE 'the file % is missing%'
                           THEN SQLERRM
                           ELSE format('%s does not load: %s (SQLSTATE %s)', file, SQLERRM, SQLSTATE) END);
    END;
END
$$;

-- Raises the load error of a file, if it had one: the first step of every test.
CREATE FUNCTION ledger_test.require(VARIADIC files text[]) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    problem text;
BEGIN
    SELECT message INTO problem FROM ledger_test.load_errors WHERE file = ANY (files) LIMIT 1;
    IF problem IS NOT NULL THEN
        RAISE EXCEPTION '%', problem;
    END IF;
END
$$;

-- Extra sessions. Each one gives up on a lock after 5 s instead of hanging the test.
CREATE FUNCTION ledger_test.open(VARIADIC names text[]) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    name text;
BEGIN
    PERFORM ledger_test.close_all();
    FOREACH name IN ARRAY names LOOP
        PERFORM dblink_connect(name, 'host=/tmp dbname=dojo user=dojo');
        PERFORM dblink_exec(name, 'SET lock_timeout = ''5s''');
    END LOOP;
END
$$;

CREATE FUNCTION ledger_test.close_all() RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    name text;
BEGIN
    FOREACH name IN ARRAY coalesce(dblink_get_connections(), '{}') LOOP
        PERFORM dblink_disconnect(name);
    END LOOP;
END
$$;

-- Deletes the rows earlier concurrency tests committed, in a session of its own. The ledger
-- refuses deletes (stage 2), so that session runs as a replica, which fires no ordinary
-- trigger; the accounts the tests added (id 1000 and up) go too.
CREATE FUNCTION ledger_test.reset() RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    tbl text;
BEGIN
    PERFORM dblink_connect('reset', 'host=/tmp dbname=dojo user=dojo');
    PERFORM dblink_exec('reset', 'SET session_replication_role = replica');
    FOREACH tbl IN ARRAY ARRAY['interest_runs', 'postings', 'journal'] LOOP
        IF to_regclass('public.' || tbl) IS NOT NULL THEN
            PERFORM dblink_exec('reset', 'DELETE FROM ' || tbl);
        END IF;
    END LOOP;
    PERFORM dblink_exec('reset', 'DELETE FROM accounts WHERE id >= 1000');
    PERFORM dblink_disconnect('reset');
END
$$;

-- Runs a statement in a session and waits: its first value as text, or 'error: ...'.
CREATE FUNCTION ledger_test.run(conn text, sql text) RETURNS text
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM dblink_send_query(conn, sql);
    RETURN ledger_test.finish(conn);
END
$$;

-- Sends a statement without waiting for it.
CREATE FUNCTION ledger_test.start(conn text, sql text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM dblink_send_query(conn, sql);
END
$$;

-- After a moment: is the session still waiting (for a lock)?
CREATE FUNCTION ledger_test.waiting(conn text) RETURNS boolean
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM pg_sleep(0.3);
    RETURN dblink_is_busy(conn) = 1;
END
$$;

-- Waits for what start() sent: its first value as text, or 'error: ...'.
CREATE FUNCTION ledger_test.finish(conn text) RETURNS text
LANGUAGE plpgsql AS $$
DECLARE
    result text;
    got boolean := false;
    r record;
BEGIN
    FOR r IN SELECT * FROM dblink_get_result(conn, false) AS t(v text) LOOP
        IF NOT got THEN
            result := r.v;
            got := true;
        END IF;
    END LOOP;
    PERFORM * FROM dblink_get_result(conn, false) AS t(v text);
    IF dblink_error_message(conn) NOT IN ('OK', '') THEN
        RETURN 'error: ' || regexp_replace(dblink_error_message(conn), '\s+$', '');
    END IF;
    RETURN result;
END
$$;

-- Fails the test with a message when a condition of a scenario does not hold.
CREATE FUNCTION ledger_test.expect(ok boolean, message text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF ok IS NOT TRUE THEN
        RAISE EXCEPTION '%', message;
    END IF;
END
$$;

-- Every file of the sidequest, in stage order, before any test: a later stage may make an
-- earlier file use the tables of a later one. A file not written yet only records its error.
SELECT ledger_test.load(file)
FROM unnest(ARRAY['ledger.sql', 'history.sql', 'transfers.sql', 'interest.sql', 'customers.sql']) WITH ORDINALITY AS f(file, n)
ORDER BY n;
