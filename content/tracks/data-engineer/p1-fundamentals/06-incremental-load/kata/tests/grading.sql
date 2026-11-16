-- The second extract, landed on 2026-11-02 at 01:00: a new version of order 3, a new order 4,
-- and the oldest version of order 1 delivered again.
\set batch2 'INSERT INTO raw_orders VALUES (3, ''paid'', 7.50, ''2026-11-01 08:00+00'', ''2026-11-02 01:00+00''), (4, ''placed'', 99.00, ''2026-11-01 09:00+00'', ''2026-11-02 01:00+00''), (1, ''placed'', 10.00, ''2026-10-31 09:00+00'', ''2026-11-02 01:00+00'')'
\set again 'SELECT dojo.run_file(''load_orders.sql'')'

SELECT dojo.script_eq(
    'the first load stores the source timestamps',
    'load_orders.sql',
    $$SELECT order_id, updated_at FROM orders ORDER BY order_id$$,
    $$VALUES (1, '2026-10-31 10:00+00'::timestamptz), (2, '2026-10-31 11:00+00'::timestamptz),
             (3, '2026-10-31 12:00+00'::timestamptz)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'the watermark moves to the newest row read',
    'load_orders.sql',
    $$SELECT watermark FROM load_state WHERE table_name = 'orders'$$,
    $$VALUES ('2026-11-01 01:00+00'::timestamptz)$$
);

SELECT dojo.script_eq(
    'running the load twice changes nothing',
    'load_orders.sql',
    $$SELECT order_id, status, amount, updated_at FROM orders ORDER BY order_id$$,
    $$VALUES (1, 'paid', 10.00, '2026-10-31 10:00+00'::timestamptz),
             (2, 'placed', 25.00, '2026-10-31 11:00+00'::timestamptz),
             (3, 'placed', 7.50, '2026-10-31 12:00+00'::timestamptz)$$,
    ordered => true,
    after => :'again'
);

SELECT dojo.script_eq(
    'an empty batch leaves the watermark where it was',
    'load_orders.sql',
    $$SELECT watermark FROM load_state WHERE table_name = 'orders'$$,
    $$VALUES ('2026-11-01 01:00+00'::timestamptz)$$,
    after => :'again'
);

SELECT dojo.script_eq(
    'the next load applies the new extract',
    'load_orders.sql',
    $$SELECT order_id, status, amount FROM orders ORDER BY order_id$$,
    $$VALUES (1, 'paid', 10.00), (2, 'placed', 25.00), (3, 'paid', 7.50), (4, 'placed', 99.00)$$,
    ordered => true,
    after => :'batch2' || '; ' || :'again'
);

SELECT dojo.script_eq(
    'the next load moves the watermark again',
    'load_orders.sql',
    $$SELECT watermark FROM load_state WHERE table_name = 'orders'$$,
    $$VALUES ('2026-11-02 01:00+00'::timestamptz)$$,
    after => :'batch2' || '; ' || :'again'
);

SELECT dojo.script_eq(
    'an old version delivered again never overwrites a newer one',
    'load_orders.sql',
    $$SELECT status, updated_at FROM orders WHERE order_id = 1$$,
    $$VALUES ('paid', '2026-10-31 10:00+00'::timestamptz)$$,
    after => :'batch2' || '; ' || :'again'
);

SELECT dojo.script_eq(
    'a newer row already in orders is kept',
    'load_orders.sql',
    $$SELECT status FROM orders WHERE order_id = 2$$,
    $$VALUES ('shipped')$$,
    setup => $$INSERT INTO orders VALUES (2, 'shipped', 25.00, '2026-11-05 00:00+00')$$
);

SELECT dojo.script_eq(
    'only rows after the watermark are read',
    'load_orders.sql',
    $$SELECT count(*) FROM orders WHERE order_id = 5$$,
    $$VALUES (0)$$,
    after => $$INSERT INTO raw_orders VALUES (5, 'placed', 1.00, '2026-10-30 00:00+00', '2026-10-31 00:00+00'); $$ || :'again'
);

SELECT dojo.script_eq(
    'versions with the same timestamp: the row that landed last wins',
    'load_orders.sql',
    $$SELECT status FROM orders WHERE order_id = 6$$,
    $$VALUES ('cancelled')$$,
    after => $$INSERT INTO raw_orders VALUES
                 (6, 'cancelled', 5.00, '2026-11-01 10:00+00', '2026-11-03 02:00+00'),
                 (6, 'placed', 5.00, '2026-11-01 10:00+00', '2026-11-03 01:00+00'); $$ || :'again'
);

SELECT dojo.script_eq(
    'an empty landing table loads nothing and keeps the watermark',
    'load_orders.sql',
    $$SELECT (SELECT count(*) FROM orders), watermark FROM load_state WHERE table_name = 'orders'$$,
    $$VALUES (0, '-infinity'::timestamptz)$$,
    setup => $$TRUNCATE raw_orders$$
);

SELECT dojo.query_eq(
    'latest_orders picks the latest version and counts the raw rows',
    'latest_orders.sql',
    $$VALUES (1, 'paid', 10.00, 3), (2, 'placed', 25.00, 2), (3, 'paid', 7.50, 2),
             (4, 'placed', 99.00, 1)$$,
    ordered => true,
    setup => :'batch2'
);
