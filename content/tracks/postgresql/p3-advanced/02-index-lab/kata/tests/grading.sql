-- Every test runs indexes.sql, then ANALYZE, then looks at plans, index types and sizes.
-- pg_temp.methods_on(table, column) lists the access methods of the indexes whose first column
-- is that column.
CREATE FUNCTION pg_temp.methods_on(tbl regclass, col text) RETURNS text[] LANGUAGE sql AS $$
    SELECT coalesce(array_agg(DISTINCT am.amname::text), '{}')
    FROM pg_index AS i
    JOIN pg_class AS c ON c.oid = i.indexrelid
    JOIN pg_am AS am ON am.oid = c.relam
    JOIN pg_attribute AS a ON a.attrelid = i.indrelid AND a.attnum = i.indkey[0]
    WHERE i.indrelid = tbl AND a.attname = col
$$;

SELECT dojo.script_ok(
    'time ranges of events use a small BRIN index',
    'indexes.sql',
    $$SELECT pg_temp.methods_on('events', 'at') = '{brin}'
         AND dojo.plan($q$SELECT * FROM events WHERE at >= '2026-11-14 23:00+00' AND at < '2026-11-15 00:00+00'$q$) !~ 'Seq Scan'$$,
    'events.at needs exactly one index, a BRIN one: the rows arrive in time order, and a B-tree on 200,000 timestamps is 100 times bigger',
    after => 'ANALYZE'
);

SELECT dojo.script_ok(
    'products by tag use a GIN index',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id FROM products WHERE tags @> ARRAY['outdoor']$q$) !~ 'Seq Scan'
         AND 'gin' = ANY (pg_temp.methods_on('products', 'tags'))$$,
    'tags @> ARRAY[...] reads the whole table: arrays need a GIN index',
    after => 'ANALYZE'
);

SELECT dojo.script_ok(
    'products by attribute use a GIN index',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id FROM products WHERE attrs @> '{"brand": "Kestrel"}'$q$) !~ 'Seq Scan'$$,
    'attrs @> ''{"brand": "Kestrel"}'' reads the whole table',
    after => 'ANALYZE'
);

SELECT dojo.script_fails(
    'an email cannot be registered twice in another case',
    'indexes.sql',
    $$INSERT INTO users (id, email) VALUES (50000, 'ana.silva@EXAMPLE.COM')$$,
    '23505',
    'ana.silva@EXAMPLE.COM is the same address as Ana.Silva@Example.com'
);

SELECT dojo.script_ok(
    'pending jobs come from a small partial index, already sorted',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id, run_at FROM jobs WHERE status = 'pending' ORDER BY run_at LIMIT 10$q$) !~ '(Seq Scan|Sort)'
         AND EXISTS (SELECT 1 FROM pg_index WHERE indrelid = 'jobs'::regclass AND indpred IS NOT NULL
                     AND pg_relation_size(indexrelid) < 65536)$$,
    'the queue reads 100 pending jobs out of 100,000: index only those (a partial index), by run_at',
    after => 'ANALYZE'
);

SELECT dojo.script_ok(
    'name fragments use a trigram index',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id FROM customers WHERE name ILIKE '%silva%'$q$) !~ 'Seq Scan'$$,
    'ILIKE ''%silva%'' reads the whole table: a B-tree cannot help with a leading wildcard',
    after => 'ANALYZE'
);

SELECT dojo.script_ok(
    'order totals of a customer need only the index',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT total FROM orders WHERE customer_id = 42$q$) ~ 'Index Only Scan'$$,
    'SELECT total ... WHERE customer_id = 42 should be an Index Only Scan: the index must hold total too',
    after => 'ANALYZE'
);

SELECT dojo.script_ok(
    'uses at most eight indexes and changes no data',
    'indexes.sql',
    $$SELECT (SELECT count(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname NOT LIKE '%\_pkey') <= 8
         AND (SELECT count(*) FROM users) = 20000 AND (SELECT count(*) FROM jobs WHERE status = 'pending') = 100$$,
    'indexes.sql should add at most eight indexes and nothing else'
);

-- The two queries, with your indexes in place.
SELECT dojo.query_eq(
    'finds the user whatever the case',
    'login.sql',
    $$VALUES (4242, 'Ana.Silva@Example.com')$$,
    setup => $$SELECT dojo.run_file('indexes.sql')$$
);

SELECT dojo.script_ok(
    'the login query uses the index',
    'indexes.sql',
    $$SELECT dojo.plan(dojo.sql_of('login.sql')) !~ 'Seq Scan'$$,
    'login.sql reads the whole users table: compare the same expression the index uses',
    after => 'ANALYZE'
);

SELECT dojo.query_eq(
    'returns the next ten pending jobs',
    'pending_jobs.sql',
    $$SELECT id, run_at FROM jobs WHERE status = 'pending' ORDER BY run_at LIMIT 10$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('indexes.sql')$$
);

SELECT dojo.script_ok(
    'the pending query uses the partial index',
    'indexes.sql',
    $$SELECT dojo.plan(dojo.sql_of('pending_jobs.sql')) !~ '(Seq Scan|Sort)'$$,
    'pending_jobs.sql reads or sorts more than it needs: filter on status = ''pending'' and order by run_at',
    after => 'ANALYZE'
);
