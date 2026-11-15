-- Most expected results come from a correct query written another way, so they follow any
-- data a test adds.
SELECT dojo.query_eq(
    'a referrer is found through any of the customers they referred',
    'no_referrals.sql',
    $$SELECT name FROM customers c WHERE c.id NOT IN (SELECT referred_by FROM customers WHERE referred_by IS NOT NULL) ORDER BY name$$,
    ordered => true,
    setup => $$UPDATE customers SET referred_by = 8 WHERE id = 6$$
);

SELECT dojo.query_eq(
    'reports every customer without multiplying rows',
    'customer_totals.sql',
    $$SELECT c.name,
             coalesce((SELECT sum(total) FROM orders o WHERE o.customer_id = c.id), 0),
             (SELECT count(*) FROM tickets t WHERE t.customer_id = c.id)
      FROM customers c ORDER BY c.name$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a customer with tickets but no orders and one with neither show zeros',
    'customer_totals.sql',
    $$VALUES ('Ada Lovelace', 0, 2), ('Dennis Ritchie', 0, 1), ('Ken Thompson', 0, 0)$$,
    ordered => true,
    setup => $$DELETE FROM orders;
                DELETE FROM tickets WHERE customer_id NOT IN (1, 7);
                UPDATE customers SET referred_by = NULL;
                DELETE FROM customers WHERE id IN (2, 3, 4, 5, 6)$$
);

SELECT dojo.query_eq(
    'returns the next page of 20 orders',
    'next_page.sql',
    $$SELECT id, placed_at, total FROM orders ORDER BY placed_at, id OFFSET 1500 LIMIT 20$$,
    ordered => true
);

SELECT dojo.query_eq(
    'orders at the same instant as the last one are split by id',
    'next_page.sql',
    $$SELECT id, placed_at, total FROM orders
      WHERE placed_at > TIMESTAMPTZ '2026-02-18 23:00+00'
         OR (placed_at = TIMESTAMPTZ '2026-02-18 23:00+00' AND id > 1500)
      ORDER BY placed_at, id LIMIT 20$$,
    ordered => true,
    setup => $$UPDATE orders SET placed_at = TIMESTAMPTZ '2026-02-18 23:00+00' WHERE id IN (1400, 2999);
                INSERT INTO orders VALUES (3001, 1, TIMESTAMPTZ '2026-02-18 23:00+00', 9.99)$$
);

SELECT dojo.script_ok(
    'the next page seeks in the index instead of skipping rows',
    NULL,
    $$SELECT dojo.plan(dojo.sql_of('next_page.sql')) !~ '(Seq Scan|Sort)'
         AND dojo.plan(dojo.sql_of('next_page.sql')) ~ 'Index Cond: .*placed_at'$$,
    'the plan reads the index from the start (or sorts): compare (placed_at, id) with the last row seen so the index can seek to it'
);

SELECT dojo.query_eq(
    'counts every order of March including March 31',
    'march_revenue.sql',
    $$VALUES (950, 46005.00)$$
);

SELECT dojo.query_eq(
    'midnight of April 1 is April, the last microsecond of March is March',
    'march_revenue.sql',
    $$SELECT count(*), sum(total) FROM orders
      WHERE date_trunc('month', placed_at) = TIMESTAMPTZ '2026-03-01 00:00+00'$$,
    setup => $$INSERT INTO orders VALUES
        (3001, 1, TIMESTAMPTZ '2026-03-31 23:59:59.999999+00', 1000.00),
        (3002, 1, TIMESTAMPTZ '2026-04-01 00:00:00+00', 2000.00),
        (3003, 1, TIMESTAMPTZ '2026-03-01 00:00:00+00', 4000.00)$$
);
