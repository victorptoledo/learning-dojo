\set again 'SELECT dojo.run_file(''apply_snapshot.sql'')'
-- The snapshot of 2026-11-15: Ada moves back to London, Linus moves to Portland and his
-- name is corrected back, Margaret becomes enterprise, Grace is missing from the extract.
\set second 'TRUNCATE customer_snapshot; INSERT INTO customer_snapshot VALUES (1, ''Ada Lovelace'', ''London'', ''retail'', ''2026-11-15''), (3, ''Linus Torvalds'', ''Portland'', ''enterprise'', ''2026-11-15''), (4, ''Margaret Hamilton'', ''Boston'', ''enterprise'', ''2026-11-15''); SELECT dojo.run_file(''apply_snapshot.sql'')'
\set history 'SELECT customer_id, name, city, segment, valid_from, valid_to, is_current FROM dim_customer ORDER BY customer_id, valid_from'

SELECT dojo.script_eq(
    'an unchanged customer keeps its versions',
    'apply_snapshot.sql',
    $$SELECT customer_key, city, valid_from, valid_to, is_current
      FROM dim_customer WHERE customer_id = 1 ORDER BY valid_from$$,
    $$VALUES (1, 'London', DATE '2026-01-01', DATE '2026-06-01', false),
             (2, 'Cambridge', DATE '2026-06-01', DATE '9999-12-31', true)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'a NULL segment that gets a value is a change',
    'apply_snapshot.sql',
    $$SELECT city, segment, valid_from, valid_to, is_current
      FROM dim_customer WHERE customer_id = 2 ORDER BY valid_from$$,
    $$VALUES ('New York', NULL, DATE '2026-01-01', DATE '2026-11-01', false),
             ('New York', 'enterprise', DATE '2026-11-01', DATE '9999-12-31', true)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'closing a version keeps its surrogate key and the new version gets a new one',
    'apply_snapshot.sql',
    $$SELECT valid_from, customer_key = 3, customer_key > 4
      FROM dim_customer WHERE customer_id = 2 ORDER BY valid_from$$,
    $$VALUES (DATE '2026-01-01', true, false), (DATE '2026-11-01', false, true)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'a name change overwrites the name without a new version',
    'apply_snapshot.sql',
    $$SELECT customer_key, name, city, is_current FROM dim_customer WHERE customer_id = 3$$,
    $$VALUES (4, 'Linus B. Torvalds', 'Helsinki', true)$$
);

SELECT dojo.script_eq(
    'applying the same snapshot twice changes nothing',
    'apply_snapshot.sql',
    :'history',
    $$VALUES (1, 'Ada Lovelace', 'London', 'retail', DATE '2026-01-01', DATE '2026-06-01', false),
             (1, 'Ada Lovelace', 'Cambridge', 'retail', DATE '2026-06-01', DATE '9999-12-31', true),
             (2, 'Grace Hopper', 'New York', NULL, DATE '2026-01-01', DATE '2026-11-01', false),
             (2, 'Grace Hopper', 'New York', 'enterprise', DATE '2026-11-01', DATE '9999-12-31', true),
             (3, 'Linus B. Torvalds', 'Helsinki', 'enterprise', DATE '2026-01-01', DATE '9999-12-31', true),
             (4, 'Margaret Hamilton', 'Boston', 'retail', DATE '2026-11-01', DATE '9999-12-31', true)$$,
    ordered => true,
    after => :'again'
);

SELECT dojo.script_eq(
    'the next snapshot adds versions where something tracked changed',
    'apply_snapshot.sql',
    $$SELECT customer_id, count(*) FROM dim_customer GROUP BY customer_id ORDER BY customer_id$$,
    $$VALUES (1, 3), (2, 2), (3, 2), (4, 2)$$,
    ordered => true,
    after => :'second'
);

SELECT dojo.script_eq(
    'a type 1 change rewrites every version, also with a type 2 change',
    'apply_snapshot.sql',
    $$SELECT name, city, valid_from, valid_to FROM dim_customer WHERE customer_id = 3
      ORDER BY valid_from$$,
    $$VALUES ('Linus Torvalds', 'Helsinki', DATE '2026-01-01', DATE '2026-11-15'),
             ('Linus Torvalds', 'Portland', DATE '2026-11-15', DATE '9999-12-31')$$,
    ordered => true,
    after => :'second'
);

SELECT dojo.script_eq(
    'a customer missing from the snapshot is left as it is',
    'apply_snapshot.sql',
    $$SELECT segment, valid_to, is_current FROM dim_customer WHERE customer_id = 2
      ORDER BY valid_from$$,
    $$VALUES (NULL, DATE '2026-11-01', false), ('enterprise', DATE '9999-12-31', true)$$,
    ordered => true,
    after => :'second'
);

SELECT dojo.script_ok(
    'versions follow each other without gaps or overlaps',
    'apply_snapshot.sql',
    $$SELECT bool_and(next_from IS NULL OR next_from = valid_to)
             AND bool_and((next_from IS NULL) = is_current)
      FROM (SELECT valid_to, is_current,
                   lead(valid_from) OVER (PARTITION BY customer_id ORDER BY valid_from) AS next_from
            FROM dim_customer) AS v$$,
    'each version must end on the day the next one starts, and only the last one is current',
    after => :'second'
);

SELECT dojo.query_eq(
    'revenue by segment uses the version valid on the order date',
    'revenue_by_segment.sql',
    $$VALUES ('enterprise', 200.00), ('retail', 150.00), ('unknown', 100.00)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'after the snapshot, new orders count for the new segment',
    'revenue_by_segment.sql',
    $$VALUES ('enterprise', 230.00), ('retail', 150.00), ('unknown', 70.00)$$,
    ordered => true,
    setup => :'again'
);
