-- Each test runs load_orders.sql on the fixture (the first extract), and maybe more after it.
SELECT dojo.script_eq(
    'the first load keeps the latest version of each order',
    'load_orders.sql',
    $$SELECT order_id, status, amount FROM orders ORDER BY order_id$$,
    $$VALUES (1, 'paid', 10.00), (2, 'placed', 25.00), (3, 'placed', 7.50)$$,
    ordered => true
);
