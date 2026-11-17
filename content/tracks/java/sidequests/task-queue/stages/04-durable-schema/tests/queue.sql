-- Stage 4 contract: schema.sql builds the table, claim.sql leases the next task.
-- Rows with a known id are inserted with OVERRIDING SYSTEM VALUE, which works for every kind of
-- generated id.

SELECT dojo.script_ok(
    'a new task gets the defaults',
    'schema.sql',
    $$(SELECT status = 'ready' AND attempts = 0 AND payload = '' AND available_at = now()
              AND lease_until IS NULL AND id IS NOT NULL
       FROM tasks WHERE type = 'email')$$,
    'INSERT INTO tasks (type) VALUES (''email'') should give status ready, 0 attempts, an empty payload and available_at now()',
    after => $$INSERT INTO tasks (type) VALUES ('email')$$
);

SELECT dojo.script_ok(
    'ids are generated and unique',
    'schema.sql',
    $$(SELECT count(DISTINCT id) = 3 FROM tasks)$$,
    'three inserts should give three ids',
    after => $$INSERT INTO tasks (type) VALUES ('a'), ('b'), ('c')$$
);

SELECT dojo.script_fails(
    'rejects an unknown status',
    'schema.sql',
    $$INSERT INTO tasks (type, status) VALUES ('email', 'lost')$$,
    '23514',
    'status must be one of ready, leased, done, dead'
);

SELECT dojo.script_fails(
    'rejects an empty type',
    'schema.sql',
    $$INSERT INTO tasks (type) VALUES ('')$$,
    '23514',
    'type must not be empty'
);

SELECT dojo.script_fails(
    'rejects a missing type',
    'schema.sql',
    $$INSERT INTO tasks (payload) VALUES ('x')$$,
    '23502',
    'type is required'
);

SELECT dojo.script_fails(
    'rejects negative attempts',
    'schema.sql',
    $$INSERT INTO tasks (type, attempts) VALUES ('email', -1)$$,
    '23514',
    'attempts can never be negative'
);

SELECT dojo.script_fails(
    'rejects a lease without lease_until',
    'schema.sql',
    $$INSERT INTO tasks (type, status) VALUES ('email', 'leased')$$,
    '23514',
    'a leased task needs lease_until'
);

SELECT dojo.script_fails(
    'rejects lease_until on a task that is not leased',
    'schema.sql',
    $$INSERT INTO tasks (type, status, lease_until) VALUES ('email', 'done', now())$$,
    '23514',
    'only a leased task has lease_until'
);

SELECT dojo.script_fails(
    'rejects a repeated idempotency key',
    'schema.sql',
    $$INSERT INTO tasks (type, idempotency_key) VALUES ('charge', 'order-7'), ('charge', 'order-7')$$,
    '23505',
    'idempotency keys are unique'
);

SELECT dojo.script_ok(
    'allows many tasks without a key',
    'schema.sql',
    $$(SELECT count(*) = 2 FROM tasks WHERE idempotency_key IS NULL)$$,
    'two tasks without a key should both be stored',
    after => $$INSERT INTO tasks (type) VALUES ('a'), ('b')$$
);

SELECT dojo.query_eq(
    'claims the oldest ready task',
    'claim.sql',
    $$VALUES (102, 'second', 'p2', 1)$$,
    setup => $$SELECT dojo.run_file('schema.sql');
               INSERT INTO tasks (id, type, payload, available_at) OVERRIDING SYSTEM VALUE VALUES
                   (101, 'first', 'p1', now() - interval '1 minute'),
                   (102, 'second', 'p2', now() - interval '1 hour'),
                   (103, 'third', 'p3', now() - interval '10 minutes')$$
);

SELECT dojo.script_ok(
    'a claimed task is leased for 30 seconds with one more attempt',
    'claim.sql',
    $$(SELECT status = 'leased' AND attempts = 3 AND lease_until = now() + interval '30 seconds'
       FROM tasks WHERE type = 'job')$$,
    'after claim.sql the task should be leased, attempts 2 -> 3, lease_until = now() + 30 s',
    setup => $$SELECT dojo.run_file('schema.sql');
               INSERT INTO tasks (type, attempts, available_at) VALUES ('job', 2, now() - interval '1 second')$$
);

SELECT dojo.script_ok(
    'skips tasks that are not available yet',
    'claim.sql',
    $$SELECT (SELECT status = 'leased' FROM tasks WHERE type = 'now')
      AND (SELECT status = 'ready' FROM tasks WHERE type = 'later')$$,
    'the task available in a minute must wait; the one available now is claimed',
    setup => $$SELECT dojo.run_file('schema.sql');
               INSERT INTO tasks (type, available_at) VALUES
                   ('later', now() + interval '1 minute'),
                   ('now', now() - interval '1 second')$$
);

SELECT dojo.query_eq(
    'claims a task whose lease ran out',
    'claim.sql',
    $$VALUES (7, 'stuck', 'p', 2)$$,
    setup => $$SELECT dojo.run_file('schema.sql');
               INSERT INTO tasks (id, type, payload, status, attempts, available_at, lease_until)
                   OVERRIDING SYSTEM VALUE VALUES
                   (7, 'stuck', 'p', 'leased', 1, now() - interval '2 minutes', now() - interval '1 minute'),
                   (8, 'busy', 'p', 'leased', 1, now() - interval '3 minutes', now() + interval '1 minute')$$
);

SELECT dojo.query_eq(
    'never claims done or dead tasks',
    'claim.sql',
    $$SELECT 1 WHERE false$$,
    setup => $$SELECT dojo.run_file('schema.sql');
               INSERT INTO tasks (type, status, available_at) VALUES
                   ('finished', 'done', now() - interval '1 hour'),
                   ('failed', 'dead', now() - interval '1 hour')$$
);

SELECT dojo.query_eq(
    'returns no row when nothing is ready',
    'claim.sql',
    $$SELECT 1 WHERE false$$,
    setup => $$SELECT dojo.run_file('schema.sql')$$
);

SELECT dojo.script_ok(
    'claims one task at a time, the smallest id on a tie',
    'claim.sql',
    $$SELECT (SELECT array_agg(type ORDER BY id) = ARRAY['a', 'b'] FROM tasks WHERE status = 'leased')
      AND (SELECT count(*) = 1 FROM tasks WHERE status = 'ready')$$,
    'running claim.sql twice should lease a then b and leave c ready',
    setup => $$SELECT dojo.run_file('schema.sql');
               INSERT INTO tasks (type, available_at) VALUES
                   ('a', now() - interval '1 minute'),
                   ('b', now() - interval '1 minute'),
                   ('c', now() - interval '1 minute')$$,
    after => $$SELECT dojo.run_file('claim.sql')$$
);
