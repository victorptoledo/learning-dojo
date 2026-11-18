-- Stage 2 contract: expired leases are claimed again until the job runs out of attempts,
-- heartbeat() extends a lease, and fail() retries with exponential backoff or kills the job.

-- Ends the lease of a job: it expired a second ago.
CREATE FUNCTION queue_test.expire(job bigint) RETURNS void
LANGUAGE sql AS $$
    UPDATE jobs SET locked_until = now() - interval '1 second' WHERE id = job
$$;

-- Claims, fails and makes the job due again, `times` times; each round lands in the temporary
-- table rounds (attempt, status, wait, last_error), wait being run_at - now().
CREATE FUNCTION queue_test.fail_rounds(times integer) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    job bigint;
    result text;
BEGIN
    CREATE TEMP TABLE rounds (attempt integer, status text, wait interval, last_error text);
    FOR i IN 1 .. times LOOP
        job := claim('mail', 'w1');
        result := fail(job, 'w1', 'timeout ' || i)::text;
        INSERT INTO rounds SELECT attempts, result, run_at - now(), last_error FROM jobs WHERE id = job;
        UPDATE jobs SET run_at = now() WHERE id = job AND status = 'pending';
    END LOOP;
END
$$;

SELECT dojo.script_ok(
    'a new job has 5 attempts and no error',
    NULL,
    $$SELECT max_attempts = 5 AND last_error IS NULL FROM jobs$$,
    'a job from enqueue should have max_attempts 5 and last_error NULL',
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}')$$
);

SELECT dojo.script_eq(
    'a live lease is not taken',
    NULL,
    $$SELECT claim('mail', 'w2'), (SELECT locked_by FROM jobs)$$,
    $$VALUES (NULL::bigint, 'w1')$$,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}');
               SELECT claim('mail', 'w1')$$
);

SELECT dojo.script_ok(
    'an expired lease is claimed again',
    NULL,
    $$SELECT (SELECT job FROM claims) = id AND status::text = 'running' AND locked_by = 'w2'
             AND attempts = 2 AND locked_until = now() + interval '30 seconds'
      FROM jobs$$,
    'after w1''s lease expired, claim(''mail'', ''w2'') should return the job, running for w2 until now() + 30 s, at its 2nd attempt',
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}');
               SELECT queue_test.expire(claim('mail', 'w1'))$$,
    after => $$SELECT queue_test.claim_times('mail', 'w2', 1)$$
);

SELECT dojo.script_eq(
    'an expired job keeps its place in line',
    NULL,
    $$SELECT j.payload ->> 'n' FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('urgent'), ('normal'), (NULL)$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{"n": "urgent"}', 9);
               SELECT queue_test.expire(claim('mail', 'w1'));
               SELECT enqueue('mail', '{"n": "normal"}')$$,
    after => $$SELECT queue_test.claim_times('mail', 'w2', 3)$$
);

SELECT dojo.script_eq(
    'an expired job with no attempts left dies',
    NULL,
    $$SELECT (SELECT job FROM claims), status::text FROM jobs$$,
    $$VALUES (NULL::bigint, 'dead')$$,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}');
               UPDATE jobs SET max_attempts = 1;
               SELECT queue_test.expire(claim('mail', 'w1'))$$,
    after => $$SELECT queue_test.claim_times('mail', 'w2', 1)$$
);

SELECT dojo.script_eq(
    'only its own queue dies',
    NULL,
    $$SELECT queue, status::text FROM jobs ORDER BY queue$$,
    $$VALUES ('images', 'running'), ('mail', 'dead')$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}');
               SELECT enqueue('images', '{}');
               UPDATE jobs SET max_attempts = 1;
               SELECT queue_test.expire(claim('mail', 'w1'));
               SELECT queue_test.expire(claim('images', 'w1'))$$,
    after => $$SELECT claim('mail', 'w2')$$
);

SELECT dojo.script_eq(
    'heartbeat extends the lease of its worker only',
    NULL,
    $$SELECT step, ok, status FROM steps ORDER BY n$$,
    $$VALUES ('heartbeat by w1', 'true', 'running'),
             ('the lease is now() + 30 s', 'true', 'running'),
             ('heartbeat by w2', 'false', 'running'),
             ('w2 claims the expired job', 'true', 'running'),
             ('late heartbeat by w1', 'false', 'running'),
             ('late complete by w1', 'false', 'running'),
             ('complete by w2', 'true', 'done'),
             ('heartbeat of a done job', 'false', 'done')$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               CREATE TEMP TABLE ids AS SELECT enqueue('mail', '{}') AS j;
               SELECT claim('mail', 'w1');
               UPDATE jobs SET locked_until = now() + interval '5 seconds'$$,
    after => $$SELECT queue_test.step('heartbeat by w1', heartbeat((SELECT j FROM ids), 'w1'), (SELECT j FROM ids));
               SELECT queue_test.step('the lease is now() + 30 s',
                   (SELECT locked_until = now() + interval '30 seconds' FROM jobs), (SELECT j FROM ids));
               SELECT queue_test.step('heartbeat by w2', heartbeat((SELECT j FROM ids), 'w2'), (SELECT j FROM ids));
               SELECT queue_test.expire((SELECT j FROM ids));
               SELECT queue_test.step('w2 claims the expired job', claim('mail', 'w2') = (SELECT j FROM ids), (SELECT j FROM ids));
               SELECT queue_test.step('late heartbeat by w1', heartbeat((SELECT j FROM ids), 'w1'), (SELECT j FROM ids));
               SELECT queue_test.step('late complete by w1', complete((SELECT j FROM ids), 'w1'), (SELECT j FROM ids));
               SELECT queue_test.step('complete by w2', complete((SELECT j FROM ids), 'w2'), (SELECT j FROM ids));
               SELECT queue_test.step('heartbeat of a done job', heartbeat((SELECT j FROM ids), 'w2'), (SELECT j FROM ids))$$
);

SELECT dojo.script_ok(
    'fail returns a job_status',
    NULL,
    $$SELECT prorettype = 'job_status'::regtype FROM pg_proc
      WHERE proname = 'fail' AND pronamespace = 'public'::regnamespace$$,
    'fail(bigint, text, text) should return job_status',
    setup => $$SELECT queue_test.require('queue.sql')$$
);

SELECT dojo.script_eq(
    'fail retries with exponential backoff, then kills the job',
    NULL,
    $$SELECT attempt, status, wait, last_error FROM rounds ORDER BY attempt$$,
    $$VALUES (1, 'pending', interval '10 seconds', 'timeout 1'),
             (2, 'pending', interval '20 seconds', 'timeout 2'),
             (3, 'pending', interval '40 seconds', 'timeout 3'),
             (4, 'pending', interval '80 seconds', 'timeout 4'),
             (5, 'dead', NULL, 'timeout 5')$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}')$$,
    after => $$SELECT queue_test.fail_rounds(5);
               UPDATE rounds SET wait = NULL WHERE status = 'dead'$$
);

SELECT dojo.script_ok(
    'fail releases the job',
    NULL,
    $$SELECT locked_by IS NULL AND locked_until IS NULL FROM jobs$$,
    'after fail, locked_by and locked_until should be NULL',
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}')$$,
    after => $$SELECT fail(claim('mail', 'w1'), 'w1', 'boom')$$
);

SELECT dojo.script_eq(
    'a failed job waits for its backoff',
    NULL,
    $$SELECT claim('mail', 'w2')$$,
    $$VALUES (NULL::bigint)$$,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}');
               SELECT fail(claim('mail', 'w1'), 'w1', 'boom')$$
);

SELECT dojo.script_eq(
    'a dead job is never claimed',
    NULL,
    $$SELECT claim('mail', 'w2'), (SELECT status::text FROM jobs)$$,
    $$VALUES (NULL::bigint, 'dead')$$,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}');
               UPDATE jobs SET max_attempts = 1;
               SELECT fail(claim('mail', 'w1'), 'w1', 'boom');
               UPDATE jobs SET run_at = now() - interval '1 hour'$$
);

SELECT dojo.script_eq(
    'fail is for the worker that holds the job',
    NULL,
    $$SELECT step, ok, status FROM steps ORDER BY n$$,
    $$VALUES ('fail by w2', NULL, 'running'),
             ('fail of a pending job', NULL, 'pending')$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               CREATE TEMP TABLE ids AS SELECT enqueue('mail', '{}', 5) AS claimed, enqueue('mail', '{}') AS waiting;
               SELECT claim('mail', 'w1')$$,
    after => $$SELECT queue_test.step('fail by w2', fail((SELECT claimed FROM ids), 'w2', 'x'), (SELECT claimed FROM ids));
               SELECT queue_test.step('fail of a pending job', fail((SELECT waiting FROM ids), 'w1', 'x'), (SELECT waiting FROM ids))$$
);
