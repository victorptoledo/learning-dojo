-- Stage 3 contract: at most one live job per queue and dedupe_key, enforced by the table, and
-- enqueue_once(), which returns the live job or enqueues one, also when two sessions race.

-- Session A enqueues a key in an open transaction, session B enqueues the same key (or
-- another one); A then ends with first_ends.
CREATE FUNCTION queue_test.two_requests(b_key text, first_ends text) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    live integer;
BEGIN
    PERFORM queue_test.reset();
    PERFORM queue_test.open('a', 'b');
    PERFORM dblink_exec('a', 'BEGIN');
    a := queue_test.run('a', 'SELECT enqueue_once(''mail'', ''invoice-42'', ''{"from": "a"}'')');
    PERFORM queue_test.expect(a IS NOT NULL AND a NOT LIKE 'error:%',
        format('session A''s enqueue_once should return a job id, got %s', coalesce(a, 'NULL')));
    PERFORM queue_test.start('b', format('SELECT enqueue_once(''mail'', %L, ''{"from": "b"}'')', b_key));
    b_waited := queue_test.waiting('b');
    PERFORM dblink_exec('a', first_ends);
    b := queue_test.finish('b');
    PERFORM queue_test.close_all();
    SELECT count(*) INTO live FROM jobs WHERE status IN ('pending', 'running');
    PERFORM queue_test.expect(b IS NOT NULL AND b NOT LIKE 'error:%',
        format('session B''s enqueue_once should return a job id, got %s', coalesce(b, 'NULL')));
    IF b_key <> 'invoice-42' THEN
        PERFORM queue_test.expect(NOT b_waited,
            'enqueueing another key waited for a transaction that enqueued invoice-42');
        PERFORM queue_test.expect(live = 2, format('there should be 2 live jobs, there are %s', live));
    ELSE
        PERFORM queue_test.expect(b_waited,
            'session B should wait for session A''s open transaction, but it finished at once with ' || b);
        IF first_ends = 'COMMIT' THEN
            PERFORM queue_test.expect(b = a, format(
                'after A committed, B should get A''s job %s, got %s', a, b));
        ELSE
            PERFORM queue_test.expect(b <> a AND EXISTS (SELECT FROM jobs WHERE id = b::bigint AND payload ->> 'from' = 'b'),
                format('after A rolled back, B should enqueue its own job, got %s', b));
        END IF;
        PERFORM queue_test.expect(live = 1, format('there should be exactly 1 live job, there are %s', live));
    END IF;
    PERFORM queue_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM queue_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_eq(
    'enqueue_once returns the live job of its key',
    NULL,
    $$SELECT a = b, a <> c, (SELECT count(*) FROM jobs), (SELECT payload ->> 'v' FROM jobs WHERE id = a),
             (SELECT dedupe_key FROM jobs WHERE id = a)
      FROM ids$$,
    $$VALUES (true, true, 2::bigint, '1', 'invoice-42')$$,
    setup => $$SELECT queue_test.require('queue.sql')$$,
    after => $$CREATE TEMP TABLE ids AS SELECT enqueue_once('mail', 'invoice-42', '{"v": 1}') AS a, 0::bigint AS b, 0::bigint AS c;
               UPDATE ids SET b = enqueue_once('mail', 'invoice-42', '{"v": 2}');
               UPDATE ids SET c = enqueue_once('mail', 'invoice-43', '{"v": 3}')$$
);

SELECT dojo.script_ok(
    'a key is per queue',
    NULL,
    $$SELECT count(*) = 2 FROM jobs$$,
    'the key invoice-42 in mail and in sms are two different jobs',
    setup => $$SELECT queue_test.require('queue.sql')$$,
    after => $$SELECT enqueue_once('mail', 'invoice-42', '{}');
               SELECT enqueue_once('sms', 'invoice-42', '{}')$$
);

SELECT dojo.script_ok(
    'a running job still holds its key',
    NULL,
    $$SELECT (SELECT job FROM claims) = (SELECT id FROM jobs) AND (SELECT count(*) = 1 FROM jobs)$$,
    'while the job of invoice-42 runs, enqueue_once with that key should return it',
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_once('mail', 'invoice-42', '{}');
               SELECT claim('mail', 'w1')$$,
    after => $$CREATE TEMP TABLE claims AS SELECT 1 AS n, enqueue_once('mail', 'invoice-42', '{}') AS job$$
);

SELECT dojo.script_eq(
    'a done or dead job frees its key',
    NULL,
    $$SELECT status::text FROM jobs ORDER BY id$$,
    $$VALUES ('done'), ('dead'), ('pending')$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_once('mail', 'invoice-42', '{}');
               SELECT complete(claim('mail', 'w1'), 'w1');
               SELECT enqueue_once('mail', 'invoice-42', '{}');
               UPDATE jobs SET max_attempts = 1 WHERE status = 'pending';
               SELECT fail(claim('mail', 'w1'), 'w1', 'boom')$$,
    after => $$SELECT enqueue_once('mail', 'invoice-42', '{}')$$
);

SELECT dojo.script_fails('the table refuses a second live job of a key', NULL,
    $$INSERT INTO jobs (queue, payload, dedupe_key) VALUES ('mail', '{}', 'invoice-42')$$, '23505',
    'a plain INSERT of a live job with the key of another live job must fail: the rule belongs to the table',
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_once('mail', 'invoice-42', '{}')$$);

SELECT dojo.script_ok(
    'jobs without a key are never compared',
    NULL,
    $$SELECT count(*) = 3 FROM jobs$$,
    'three enqueue() calls without a key should make three jobs',
    setup => $$SELECT queue_test.require('queue.sql')$$,
    after => $$SELECT enqueue('mail', '{}') FROM generate_series(1, 3)$$
);

SELECT dojo.script_ok('two requests with one key make one job', NULL,
    $$SELECT queue_test.two_requests('invoice-42', 'COMMIT')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);

SELECT dojo.script_ok('a rolled back request leaves the key to the other', NULL,
    $$SELECT queue_test.two_requests('invoice-42', 'ROLLBACK')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);

SELECT dojo.script_ok('two keys do not wait for each other', NULL,
    $$SELECT queue_test.two_requests('invoice-43', 'COMMIT')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);
