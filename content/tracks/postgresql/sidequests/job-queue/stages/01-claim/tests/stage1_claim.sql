-- Stage 1 contract: queue.sql creates jobs, enqueue(), claim() and complete(); two workers
-- claiming at once never wait for each other or take the same job.

-- Worker A claims in an open transaction, worker B claims too; A then ends with first_ends,
-- and B claims once more. The queue holds `jobs` jobs, enqueued in id order.
CREATE FUNCTION queue_test.two_workers(jobs integer, first_ends text) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    ids bigint[];
    a text;
    b text;
    b_waited boolean;
    again text;
BEGIN
    PERFORM queue_test.reset();
    PERFORM queue_test.open('a', 'b');
    FOR i IN 1 .. jobs LOOP
        ids := ids || queue_test.run('a', format('SELECT enqueue(''mail'', ''{"n": %s}'')', i))::bigint;
    END LOOP;
    PERFORM dblink_exec('a', 'BEGIN');
    a := queue_test.run('a', 'SELECT claim(''mail'', ''worker-a'')');
    PERFORM queue_test.expect(a = ids[1]::text,
        format('worker A should claim the first job (%s), got %s', ids[1], coalesce(a, 'NULL')));
    PERFORM queue_test.start('b', 'SELECT claim(''mail'', ''worker-b'')');
    b_waited := queue_test.waiting('b');
    PERFORM dblink_exec('a', first_ends);
    b := queue_test.finish('b');
    again := queue_test.run('b', 'SELECT claim(''mail'', ''worker-b'')');
    PERFORM queue_test.close_all();
    PERFORM queue_test.expect(NOT b_waited,
        'worker B waited for worker A''s open transaction: it should skip the job A is taking');
    PERFORM queue_test.expect(b IS NOT DISTINCT FROM ids[2]::text, format(
        'while worker A holds job %s, worker B should get %s, got %s',
        ids[1], coalesce(ids[2]::text, 'NULL (no other job)'), coalesce(b, 'NULL')));
    IF first_ends = 'COMMIT' THEN
        PERFORM queue_test.expect(again IS NOT DISTINCT FROM ids[3]::text, format(
            'after A committed, the next claim should get %s, got %s',
            coalesce(ids[3]::text, 'NULL (nothing left)'), coalesce(again, 'NULL')));
    ELSE
        PERFORM queue_test.expect(again = ids[1]::text, format(
            'A rolled back its claim, so job %s is pending again and should be the next one, got %s',
            ids[1], coalesce(again, 'NULL')));
    END IF;
    PERFORM queue_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM queue_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_ok(
    'enqueue adds a pending job',
    NULL,
    $$SELECT queue = 'mail' AND payload = '{"to": "ana@example.com"}' AND priority = 0
             AND run_at = now() AND status::text = 'pending' AND attempts = 0
             AND locked_by IS NULL AND locked_until IS NULL
      FROM jobs WHERE id = (SELECT id FROM r)$$,
    'enqueue(''mail'', ''{"to": "ana@example.com"}'') should return the id of a pending job with priority 0, run_at now() and 0 attempts',
    setup => $$SELECT queue_test.require('queue.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT enqueue('mail', '{"to": "ana@example.com"}') AS id$$
);

SELECT dojo.script_ok(
    'the job statuses',
    NULL,
    $$SELECT enum_range(NULL::job_status)::text[] = '{pending,running,done,dead}'$$,
    'job_status should be an enum of pending, running, done and dead, in that order',
    setup => $$SELECT queue_test.require('queue.sql')$$
);

SELECT dojo.script_ok(
    'claim takes the job and leases it',
    NULL,
    $$SELECT (SELECT id FROM r) = (SELECT min(id) FROM jobs)
         AND (SELECT status::text = 'running' AND locked_by = 'w1' AND attempts = 1
                     AND locked_until = now() + interval '30 seconds'
              FROM jobs WHERE id = (SELECT id FROM r))$$,
    'claim(''mail'', ''w1'') should return the job, now running, locked by w1 until now() + 30 s, with 1 attempt',
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}')$$,
    after => $$CREATE TEMP TABLE r AS SELECT claim('mail', 'w1') AS id$$
);

SELECT dojo.script_eq(
    'claim follows priority, then run_at, then id',
    NULL,
    $$SELECT j.payload ->> 'n' FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('urgent'), ('older'), ('first'), ('second'), (NULL), (NULL)$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{"n": "first"}', 0, now() - interval '1 minute');
               SELECT enqueue('mail', '{"n": "second"}', 0, now() - interval '1 minute');
               SELECT enqueue('mail', '{"n": "older"}', 5, now() - interval '2 hours');
               SELECT enqueue('mail', '{"n": "urgent"}', 9);
               SELECT enqueue('mail', '{"n": "later"}', 9, now() + interval '1 minute')$$,
    after => $$SELECT queue_test.claim_times('mail', 'w1', 6)$$
);

SELECT dojo.script_eq(
    'claim takes only its own queue',
    NULL,
    $$SELECT j.queue FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('mail'), (NULL)$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('images', '{}', 9);
               SELECT enqueue('mail', '{}')$$,
    after => $$SELECT queue_test.claim_times('mail', 'w1', 2)$$
);

SELECT dojo.script_eq(
    'claim on an empty queue returns NULL',
    NULL,
    $$SELECT claim('sms', 'w1')$$,
    $$VALUES (NULL::bigint)$$,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue('mail', '{}')$$
);

SELECT dojo.script_eq(
    'complete is for the worker that holds the job',
    NULL,
    $$SELECT step, ok, status FROM steps ORDER BY n$$,
    $$VALUES ('complete by w2', 'false', 'running'),
             ('complete by w1', 'true', 'done'),
             ('complete again', 'false', 'done'),
             ('complete a pending job', 'false', 'pending')$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               CREATE TEMP TABLE ids AS SELECT enqueue('mail', '{}', 5) AS claimed, enqueue('mail', '{}') AS waiting;
               SELECT claim('mail', 'w1')$$,
    after => $$SELECT queue_test.step('complete by w2', complete((SELECT claimed FROM ids), 'w2'), (SELECT claimed FROM ids));
               SELECT queue_test.step('complete by w1', complete((SELECT claimed FROM ids), 'w1'), (SELECT claimed FROM ids));
               SELECT queue_test.step('complete again', complete((SELECT claimed FROM ids), 'w1'), (SELECT claimed FROM ids));
               SELECT queue_test.step('complete a pending job', complete((SELECT waiting FROM ids), 'w1'), (SELECT waiting FROM ids))$$
);

SELECT dojo.script_ok('two workers take two jobs without waiting', NULL,
    $$SELECT queue_test.two_workers(3, 'COMMIT')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);

SELECT dojo.script_ok('a rolled back claim gives the job back', NULL,
    $$SELECT queue_test.two_workers(3, 'ROLLBACK')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);

SELECT dojo.script_ok('a second worker finds nothing at once', NULL,
    $$SELECT queue_test.two_workers(1, 'COMMIT')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);

SELECT dojo.script_ok(
    'a claim stays fast with 200000 done jobs',
    NULL,
    $$SELECT queue_test.buffers($q$SELECT claim('mail', 'w1')$q$) <= 60$$,
    'a second claim(''mail'', ''w1'') touched more than 60 buffers with 200,000 done jobs in the table: it should find the next pending job without reading the done ones',
    setup => $$SELECT queue_test.require('queue.sql');
               INSERT INTO jobs (queue, payload, status, attempts, run_at)
               SELECT CASE WHEN i % 3 = 0 THEN 'images' ELSE 'mail' END, jsonb_build_object('n', i),
                      'done', 1, now() - interval '1 day' + i * interval '100 ms'
               FROM generate_series(1, 200000) AS i;
               SELECT enqueue('mail', '{}', i % 3) FROM generate_series(1, 20) AS i;
               SELECT enqueue('images', '{}') FROM generate_series(1, 20);
               ANALYZE jobs;
               -- A first claim plans the function's statements; the measured one runs them.
               SELECT claim('mail', 'w0')$$
);
