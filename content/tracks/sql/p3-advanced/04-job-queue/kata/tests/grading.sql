-- A second session, opened with dblink, plays another worker that holds a lock.
CREATE EXTENSION IF NOT EXISTS dblink;
CREATE FUNCTION pg_temp.other_worker_locks(job bigint) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF 'other' = ANY (coalesce(dblink_get_connections(), '{}')) THEN
        PERFORM dblink_disconnect('other');
    END IF;
    PERFORM dblink_connect('other', 'host=/tmp dbname=dojo user=dojo');
    PERFORM dblink_exec('other', 'BEGIN');
    PERFORM * FROM dblink('other', format('SELECT id FROM jobs WHERE id = %s FOR UPDATE', job)) AS t(id bigint);
END
$$;

SELECT dojo.script_eq(
    'marks the claimed job as running for worker 1',
    'claim.sql',
    $$SELECT id, status, locked_by, locked_at, attempts FROM jobs WHERE status = 'running' AND locked_by = 'worker-1'$$,
    $$VALUES (3, 'running', 'worker-1', TIMESTAMPTZ '2026-11-15 12:00+00', 1),
             (9, 'running', 'worker-1', TIMESTAMPTZ '2026-11-15 11:40+00', 1)$$
);

SELECT dojo.query_eq(
    'equal priorities go oldest first and future jobs wait',
    'claim.sql',
    $$VALUES (2, 1)$$,
    setup => $$UPDATE jobs SET status = 'done' WHERE id IN (3, 4);
                UPDATE jobs SET priority = 0 WHERE id = 5$$
);

SELECT dojo.query_eq(
    'claims only email jobs that are ready',
    'claim.sql',
    $$SELECT 1, 1 WHERE false$$,
    setup => $$UPDATE jobs SET status = 'done' WHERE queue = 'emails' AND status = 'ready' AND run_at <= TIMESTAMPTZ '2026-11-15 12:00+00'$$
);

SELECT dojo.query_eq(
    'skips a job another worker holds instead of waiting',
    'claim.sql',
    $$VALUES (4, 1)$$,
    setup => $$SET LOCAL lock_timeout = '2s'; SELECT pg_temp.other_worker_locks(3)$$
);
-- The other worker ends its transaction and goes away.
SELECT dblink_disconnect('other');

SELECT dojo.query_eq(
    'retries a failed job with backoff',
    'fail.sql',
    $$VALUES (7, 'ready', TIMESTAMPTZ '2026-11-15 12:01:00+00')$$
);

SELECT dojo.script_eq(
    'a retried job is unlocked and keeps its error',
    'fail.sql',
    $$SELECT status, locked_by, locked_at, last_error, attempts FROM jobs WHERE id = 7$$,
    $$VALUES ('ready', NULL::text, NULL::timestamptz, 'smtp timeout', 1)$$
);

SELECT dojo.query_eq(
    'the backoff doubles with each attempt',
    'fail.sql',
    $$VALUES (7, 'ready', TIMESTAMPTZ '2026-11-15 12:02:00+00')$$,
    setup => $$UPDATE jobs SET attempts = 2 WHERE id = 7$$
);

SELECT dojo.query_eq(
    'a job that used all its attempts is dead',
    'fail.sql',
    $$VALUES (7, 'dead', TIMESTAMPTZ '2026-11-15 11:50+00')$$,
    setup => $$UPDATE jobs SET attempts = 3 WHERE id = 7$$
);

SELECT dojo.script_eq(
    'releases only the jobs locked more than 5 minutes ago',
    'reap.sql',
    $$SELECT id, status, locked_by FROM jobs WHERE id IN (7, 8, 9)$$,
    $$VALUES (7, 'running', 'worker-2'), (8, 'running', 'worker-3'), (9, 'ready', NULL)$$
);

SELECT dojo.query_eq(
    'returns the ids of the released jobs',
    'reap.sql',
    $$VALUES (8), (9)$$,
    setup => $$UPDATE jobs SET locked_at = TIMESTAMPTZ '2026-11-15 11:54:59+00' WHERE id = 8$$
);

SELECT dojo.query_eq(
    'reports every queue',
    'queue_stats.sql',
    $$VALUES ('emails', 3, 1, 2, 0), ('reports', 1, 0, 1, 1)$$,
    ordered => true
);
