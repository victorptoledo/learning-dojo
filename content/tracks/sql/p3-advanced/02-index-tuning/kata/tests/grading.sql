SELECT dojo.script_ok(
    'the latest orders of a customer come from an index in order',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id, placed_at, total FROM orders WHERE customer_id = 42 ORDER BY placed_at DESC LIMIT 10$q$)
             !~ '(Seq Scan|Sort|Filter)'
         AND current_setting('enable_seqscan') = 'on' AND current_setting('enable_sort') = 'on'$$,
    'the plan still filters, sorts or scans the whole table: put the equality column first and the sort column after it',
    after => 'ANALYZE'
);

SELECT dojo.script_ok(
    'the oldest pending orders come from an index in order',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id, placed_at FROM orders WHERE status = 'pending' ORDER BY placed_at LIMIT 50$q$)
             !~ '(Seq Scan|Sort|Filter)'
         AND current_setting('enable_seqscan') = 'on' AND current_setting('enable_sort') = 'on'$$,
    'the plan still reads non-pending orders and throws them away (a Filter): the index must know the status',
    after => 'ANALYZE'
);

SELECT dojo.script_ok(
    'uses at most three new indexes',
    'indexes.sql',
    $$SELECT count(*) <= 3 FROM pg_indexes
      WHERE schemaname = 'public' AND indexname NOT LIKE '%\_pkey' AND indexname <> 'orders_placed_at'$$,
    'more than three new indexes: every index slows down every write'
);

SELECT dojo.script_ok(
    'only creates indexes and changes no data',
    'indexes.sql',
    $$SELECT (SELECT count(*) FROM orders) = 200000
         AND (SELECT count(*) FROM customers) = 50000
         AND (SELECT sum(total) FROM orders) = (SELECT sum((i % 500) + 0.99) FROM generate_series(1, 200000) AS i)
         AND EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'orders_placed_at')$$,
    'indexes.sql must only add indexes'
);

SELECT dojo.query_eq(
    'counts the paid orders of March like the slow query',
    'march_orders.sql',
    $$SELECT count(*), sum(total) FROM orders
      WHERE date_trunc('month', placed_at) = TIMESTAMPTZ '2025-03-01 00:00+00' AND status = 'paid'$$
);

SELECT dojo.query_eq(
    'includes the first instant of March and excludes the first of April',
    'march_orders.sql',
    $$SELECT count(*), sum(total) FROM orders
      WHERE date_trunc('month', placed_at) = TIMESTAMPTZ '2025-03-01 00:00+00' AND status = 'paid'$$,
    setup => $$INSERT INTO orders VALUES
        (200001, 1, 'paid', TIMESTAMPTZ '2025-03-01 00:00:00+00', 1000.00),
        (200002, 1, 'paid', TIMESTAMPTZ '2025-03-31 23:59:59.999999+00', 2000.00),
        (200003, 1, 'paid', TIMESTAMPTZ '2025-04-01 00:00:00+00', 4000.00),
        (200004, 1, 'paid', TIMESTAMPTZ '2025-02-28 23:59:59.999999+00', 8000.00)$$
);

SELECT dojo.script_ok(
    'the March query can use the index on placed_at',
    NULL,
    $$SELECT dojo.plan(dojo.sql_of('march_orders.sql')) !~ 'Seq Scan'
         AND dojo.plan(dojo.sql_of('march_orders.sql')) ~ 'orders_placed_at'$$,
    'the plan still reads every order: keep placed_at alone on one side of the comparison'
);
