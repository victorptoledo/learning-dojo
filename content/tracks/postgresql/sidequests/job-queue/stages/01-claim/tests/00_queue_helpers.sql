-- Helpers of the job-queue tests (no tests here). Loaded before the stage files.
--
-- The learner's files are loaded once and committed (at the end of this file), because the
-- concurrency tests open real extra sessions with dblink, and those only see committed
-- objects. A failed load is remembered and reported by every test that needs the file.
-- The other tests run inside the usual rolled-back block.

CREATE EXTENSION dblink;
CREATE SCHEMA queue_test;

CREATE TABLE queue_test.load_errors (
    file    text PRIMARY KEY,
    message text NOT NULL
);

CREATE FUNCTION queue_test.load(file text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    BEGIN
        EXECUTE dojo.sql_of(file);
    EXCEPTION WHEN OTHERS THEN
        INSERT INTO queue_test.load_errors
        VALUES (file, CASE WHEN SQLERRM LIKE 'todo:%' OR SQLERRM LIKE 'the file % is missing%'
                           THEN SQLERRM
                           ELSE format('%s does not load: %s (SQLSTATE %s)', file, SQLERRM, SQLSTATE) END);
    END;
END
$$;

-- Raises the load error of a file, if it had one: the first step of every test.
CREATE FUNCTION queue_test.require(VARIADIC files text[]) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    problem text;
BEGIN
    SELECT message INTO problem FROM queue_test.load_errors WHERE file = ANY (files) LIMIT 1;
    IF problem IS NOT NULL THEN
        RAISE EXCEPTION '%', problem;
    END IF;
END
$$;

-- Extra sessions. Each one gives up on a lock after 5 s instead of hanging the test.
CREATE FUNCTION queue_test.open(VARIADIC names text[]) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    name text;
BEGIN
    PERFORM queue_test.close_all();
    FOREACH name IN ARRAY names LOOP
        PERFORM dblink_connect(name, 'host=/tmp dbname=dojo user=dojo');
        PERFORM dblink_exec(name, 'SET lock_timeout = ''5s''');
    END LOOP;
END
$$;

CREATE FUNCTION queue_test.close_all() RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    name text;
BEGIN
    FOREACH name IN ARRAY coalesce(dblink_get_connections(), '{}') LOOP
        PERFORM dblink_disconnect(name);
    END LOOP;
END
$$;

-- Deletes the rows earlier concurrency tests committed, in a session of its own.
CREATE FUNCTION queue_test.reset() RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    tbl text;
BEGIN
    PERFORM dblink_connect('reset', 'host=/tmp dbname=dojo user=dojo');
    FOREACH tbl IN ARRAY ARRAY['jobs'] LOOP
        IF to_regclass('public.' || tbl) IS NOT NULL THEN
            PERFORM dblink_exec('reset', 'DELETE FROM ' || tbl);
        END IF;
    END LOOP;
    PERFORM dblink_disconnect('reset');
END
$$;

-- Runs a statement in a session and waits: its first value as text, or 'error: ...'.
CREATE FUNCTION queue_test.run(conn text, sql text) RETURNS text
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM dblink_send_query(conn, sql);
    RETURN queue_test.finish(conn);
END
$$;

-- Sends a statement without waiting for it.
CREATE FUNCTION queue_test.start(conn text, sql text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM dblink_send_query(conn, sql);
END
$$;

-- After a moment: is the session still waiting (for a lock)?
CREATE FUNCTION queue_test.waiting(conn text) RETURNS boolean
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM pg_sleep(0.3);
    RETURN dblink_is_busy(conn) = 1;
END
$$;

-- Waits for what start() sent: its first value as text, or 'error: ...'.
CREATE FUNCTION queue_test.finish(conn text) RETURNS text
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
CREATE FUNCTION queue_test.expect(ok boolean, message text) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF ok IS NOT TRUE THEN
        RAISE EXCEPTION '%', message;
    END IF;
END
$$;

-- The shared buffers (hit + read) a statement touches, functions it calls included.
CREATE FUNCTION queue_test.buffers(query text) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    plan json;
BEGIN
    EXECUTE 'EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) ' || query INTO plan;
    RETURN (plan -> 0 -> 'Plan' ->> 'Shared Hit Blocks')::bigint
         + (plan -> 0 -> 'Plan' ->> 'Shared Read Blocks')::bigint;
END
$$;

-- Claims `times` jobs one after the other; the ids (or NULLs) land in the temporary table
-- claims (n, job).
CREATE FUNCTION queue_test.claim_times(q text, worker text, times integer) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    CREATE TEMP TABLE IF NOT EXISTS claims (n serial, job bigint);
    FOR i IN 1 .. times LOOP
        INSERT INTO claims (job) VALUES (claim(q, worker));
    END LOOP;
END
$$;

-- Records what a call returned and the job's status right after it, in the temporary table
-- steps (n, step, ok, status).
CREATE FUNCTION queue_test.step(step text, ok anyelement, job bigint) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    CREATE TEMP TABLE IF NOT EXISTS steps (n serial, step text, ok text, status text);
    INSERT INTO steps (step, ok, status) VALUES (step, ok::text, (SELECT status::text FROM jobs WHERE id = job));
END
$$;

-- The learner's file, before any test (every stage changes the same file). If it does not
-- load, only its error is recorded.
SELECT queue_test.load('queue.sql');
