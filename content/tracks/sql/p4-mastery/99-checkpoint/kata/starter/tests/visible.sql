-- Tests of migrate.sql run your file, then check the tables it leaves behind.
SELECT dojo.script_eq(
    'fills subscriptions from the plan lists',
    'migrate.sql',
    $$SELECT customer_id, plan_id FROM subscriptions ORDER BY customer_id, plan_id$$,
    $$VALUES (1, 2), (1, 4), (2, 1), (3, 3), (3, 4), (3, 5), (5, 1), (6, 2), (8, 1)$$,
    ordered => true
);
