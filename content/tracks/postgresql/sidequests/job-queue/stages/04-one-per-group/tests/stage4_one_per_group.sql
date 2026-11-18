-- Stage 4 contract: enqueue_grouped() and a claim() that runs one job per group at a time,
-- also when the group's first claim is not committed yet.

-- Jobs 1 and 2 of acme, job 3 of globex. Worker A claims in an open transaction, worker B
-- claims; A ends with first_ends and B claims again.
CREATE FUNCTION queue_test.two_workers_two_groups(first_ends text) RETURNS boolean
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
    ids := ids || queue_test.run('a', 'SELECT enqueue_grouped(''sync'', ''acme'', ''{"n": 1}'')')::bigint;
    ids := ids || queue_test.run('a', 'SELECT enqueue_grouped(''sync'', ''acme'', ''{"n": 2}'')')::bigint;
    ids := ids || queue_test.run('a', 'SELECT enqueue_grouped(''sync'', ''globex'', ''{"n": 3}'')')::bigint;
    PERFORM dblink_exec('a', 'BEGIN');
    a := queue_test.run('a', 'SELECT claim(''sync'', ''worker-a'')');
    PERFORM queue_test.expect(a = ids[1]::text,
        format('worker A should claim acme''s first job (%s), got %s', ids[1], coalesce(a, 'NULL')));
    PERFORM queue_test.start('b', 'SELECT claim(''sync'', ''worker-b'')');
    b_waited := queue_test.waiting('b');
    b := queue_test.finish('b');
    PERFORM dblink_exec('a', first_ends);
    again := queue_test.run('b', 'SELECT claim(''sync'', ''worker-b'')');
    PERFORM queue_test.close_all();
    PERFORM queue_test.expect(NOT b_waited,
        'worker B waited for worker A''s open transaction: a claim never waits');
    PERFORM queue_test.expect(b = ids[3]::text, format(
        'while worker A is claiming acme''s job %s, worker B should skip acme and get globex''s job %s, got %s%s',
        ids[1], ids[3], coalesce(b, 'NULL'),
        CASE WHEN b = ids[2]::text THEN ' (acme''s second job: acme would run twice at once)' ELSE '' END));
    IF first_ends = 'COMMIT' THEN
        PERFORM queue_test.expect(again IS NULL, format(
            'acme''s first job is running and globex''s is taken: the next claim should return NULL, got %s', again));
    ELSE
        PERFORM queue_test.expect(again = ids[1]::text, format(
            'A rolled back its claim, so acme''s first job (%s) should be the next one, got %s',
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
    'enqueue_grouped adds a pending job of the group',
    NULL,
    $$SELECT queue = 'sync' AND group_key = 'acme' AND payload = '{"n": 1}' AND status::text = 'pending'
             AND priority = 0 AND run_at = now()
      FROM jobs WHERE id = (SELECT id FROM r)$$,
    'enqueue_grouped(''sync'', ''acme'', ''{"n": 1}'') should return the id of a pending job of group acme',
    setup => $$SELECT queue_test.require('queue.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT enqueue_grouped('sync', 'acme', '{"n": 1}') AS id$$
);

SELECT dojo.script_eq(
    'a group runs one job at a time',
    NULL,
    $$SELECT j.payload ->> 'n' FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('acme 1'), ('globex 1'), ('no group 1'), ('no group 2'), (NULL)$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 1"}');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 2"}');
               SELECT enqueue_grouped('sync', 'globex', '{"n": "globex 1"}');
               SELECT enqueue('sync', '{"n": "no group 1"}');
               SELECT enqueue_grouped('sync', 'globex', '{"n": "globex 2"}');
               SELECT enqueue('sync', '{"n": "no group 2"}')$$,
    after => $$SELECT queue_test.claim_times('sync', 'w1', 5)$$
);

SELECT dojo.script_eq(
    'a finished job lets its group go on',
    NULL,
    $$SELECT j.payload ->> 'n' FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('acme 2'), (NULL)$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 1"}');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 2"}');
               SELECT complete(claim('sync', 'w1'), 'w1')$$,
    after => $$SELECT queue_test.claim_times('sync', 'w1', 2)$$
);

SELECT dojo.script_eq(
    'a failed job lets its group go on',
    NULL,
    $$SELECT j.payload ->> 'n' FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('acme 2'), (NULL)$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 1"}');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 2"}');
               SELECT fail(claim('sync', 'w1'), 'w1', 'boom')$$,
    after => $$SELECT queue_test.claim_times('sync', 'w1', 2)$$
);

SELECT dojo.script_eq(
    'a dead lease does not hold its group',
    NULL,
    $$SELECT j.payload ->> 'n' FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('acme 2'), (NULL)$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 1"}');
               SELECT enqueue_grouped('sync', 'acme', '{"n": "acme 2"}');
               UPDATE jobs SET max_attempts = 1;
               SELECT queue_test.expire(claim('sync', 'w1'))$$,
    after => $$SELECT queue_test.claim_times('sync', 'w2', 2)$$
);

SELECT dojo.script_eq(
    'a group is per queue',
    NULL,
    $$SELECT j.queue FROM claims c LEFT JOIN jobs j ON j.id = c.job ORDER BY c.n$$,
    $$VALUES ('images')$$,
    ordered => true,
    setup => $$SELECT queue_test.require('queue.sql');
               SELECT enqueue_grouped('sync', 'acme', '{}');
               SELECT enqueue_grouped('images', 'acme', '{}');
               SELECT claim('sync', 'w1')$$,
    after => $$SELECT queue_test.claim_times('images', 'w2', 1)$$
);

SELECT dojo.script_ok('a group being claimed is skipped without waiting', NULL,
    $$SELECT queue_test.two_workers_two_groups('COMMIT')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);

SELECT dojo.script_ok('a rolled back claim frees its group', NULL,
    $$SELECT queue_test.two_workers_two_groups('ROLLBACK')$$, 'see above',
    setup => $$SELECT queue_test.require('queue.sql')$$);
