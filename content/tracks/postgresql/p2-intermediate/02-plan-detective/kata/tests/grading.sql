-- The planner's row estimate for a query, read from EXPLAIN (FORMAT JSON).
CREATE FUNCTION pg_temp.estimate(query text) RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
    plan json;
BEGIN
    EXECUTE 'EXPLAIN (FORMAT JSON) ' || query INTO plan;
    RETURN (plan -> 0 -> 'Plan' ->> 'Plan Rows')::bigint;
END
$$;

-- statistics.sql
SELECT dojo.script_ok(
    'estimates a city with its own country close to reality',
    'statistics.sql',
    $$SELECT pg_temp.estimate($q$SELECT * FROM addresses WHERE city = 'Porto' AND country = 'PT'$q$) BETWEEN 800 AND 1250$$,
    'the estimate for Porto in PT is still far from the real 1,000 rows: the planner treats city and country as independent'
);

SELECT dojo.script_ok(
    'estimates another city as well',
    'statistics.sql',
    $$SELECT pg_temp.estimate($q$SELECT * FROM addresses WHERE country = 'IT' AND city = 'Rome'$q$) BETWEEN 800 AND 1250$$,
    'the estimate for Rome in IT is far from the real 1,000 rows'
);

SELECT dojo.script_ok(
    'estimates an impossible pair as almost nothing',
    'statistics.sql',
    $$SELECT pg_temp.estimate($q$SELECT * FROM addresses WHERE city = 'Porto' AND country = 'ES'$q$) <= 10$$,
    'Porto is never in ES, but the estimate is high: functional dependencies alone assume the pair matches; the combinations themselves need a multi-column MCV list'
);

SELECT dojo.script_ok(
    'uses extended statistics on the two columns',
    'statistics.sql',
    $$SELECT EXISTS (
          SELECT 1 FROM pg_statistic_ext AS s
          WHERE s.stxrelid = 'addresses'::regclass
            AND (SELECT array_agg(a.attname::text ORDER BY a.attname)
                 FROM unnest(s.stxkeys) AS k(attnum)
                 JOIN pg_attribute AS a ON a.attrelid = s.stxrelid AND a.attnum = k.attnum) = '{city,country}')$$,
    'no CREATE STATISTICS object covers city and country of addresses'
);

SELECT dojo.script_ok(
    'adds no index and changes no data',
    'statistics.sql',
    $$SELECT (SELECT count(*) FROM pg_indexes WHERE tablename = 'addresses') = 1
         AND (SELECT count(*) FROM addresses WHERE city = 'Porto' AND country = 'PT') = 1000$$,
    'statistics.sql should only add statistics'
);

-- common_status.sql
SELECT dojo.query_eq(
    'follows the statistics after the data changes',
    'common_status.sql',
    $$VALUES ('paid', 0.5), ('shipped', 0.35), ('new', 0.1), ('cancelled', 0.04), ('refunded', 0.01)$$,
    ordered => true,
    setup => $$UPDATE orders SET status = 'shipped' WHERE status = 'paid' AND id % 100 < 10;
                ANALYZE orders$$
);

SELECT dojo.query_eq(
    'reads the statistics of public.orders, not of another orders',
    'common_status.sql',
    $$VALUES ('paid', 0.6), ('shipped', 0.25), ('new', 0.1), ('cancelled', 0.04), ('refunded', 0.01)$$,
    ordered => true,
    setup => $$CREATE SCHEMA archive;
                CREATE TABLE archive.orders AS SELECT id, 'archived'::text AS status FROM generate_series(1, 1000) AS id;
                ANALYZE archive.orders$$
);

-- worst_estimate.sql
SELECT dojo.query_eq(
    'finds the worst estimate of each plan',
    'worst_estimate.sql',
    $$VALUES (1, 'Nested Loop', NULL::text, 12, 144630),
             (2, 'Bitmap Heap Scan', 'customers', 2500, 290),
             (3, 'Seq Scan', 'orders', 300, 0)$$
);

SELECT dojo.query_eq(
    'looks at every level of a deep plan',
    'worst_estimate.sql',
    $$VALUES (1, 'Nested Loop', NULL::text, 12, 144630),
             (2, 'Bitmap Heap Scan', 'customers', 2500, 290),
             (3, 'Seq Scan', 'orders', 300, 0),
             (4, 'Index Only Scan', 'order_items', 2, 900)$$,
    setup => $$INSERT INTO captured_plans VALUES (4, 'deep',
                '[{"Plan": {"Node Type": "Limit", "Plan Rows": 10, "Actual Rows": 10, "Actual Loops": 1,
                   "Plans": [{"Node Type": "Sort", "Plan Rows": 40, "Actual Rows": 10, "Actual Loops": 1,
                    "Plans": [{"Node Type": "Hash Join", "Plan Rows": 40, "Actual Rows": 900, "Actual Loops": 1,
                     "Plans": [{"Node Type": "Seq Scan", "Relation Name": "orders", "Plan Rows": 30000, "Actual Rows": 30000, "Actual Loops": 1},
                               {"Node Type": "Hash", "Plan Rows": 3, "Actual Rows": 900, "Actual Loops": 1,
                                "Plans": [{"Node Type": "Index Only Scan", "Relation Name": "order_items", "Plan Rows": 2, "Actual Rows": 900, "Actual Loops": 1}]}]}]}]}}]')$$
);

SELECT dojo.query_eq(
    'an overestimate counts as much as an underestimate',
    'worst_estimate.sql',
    $$VALUES (1, 'Nested Loop', NULL::text, 12, 144630),
             (2, 'Bitmap Heap Scan', 'customers', 2500, 290),
             (3, 'Seq Scan', 'orders', 300, 0),
             (5, 'Index Scan', 'events', 100000, 50)$$,
    setup => $$INSERT INTO captured_plans VALUES (5, 'over',
                '[{"Plan": {"Node Type": "Nested Loop", "Plan Rows": 50, "Actual Rows": 4000, "Actual Loops": 1,
                   "Plans": [{"Node Type": "Index Scan", "Relation Name": "events", "Plan Rows": 100000, "Actual Rows": 50, "Actual Loops": 1},
                             {"Node Type": "Index Scan", "Relation Name": "users", "Plan Rows": 1, "Actual Rows": 80, "Actual Loops": 50}]}}]')$$
);
