-- The index tests come first: they build indexes on products before any test has updated it.
SELECT dojo.script_ok(
    'the color search reads an index',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id, sku FROM products WHERE attrs @> '{"color": "red"}' ORDER BY id$q$)
             !~ 'Seq Scan'$$,
    'the plan of the color search still has a Seq Scan: a B-tree cannot answer @>, but a GIN index on attrs can'
);

SELECT dojo.script_ok(
    'the brand page reads an index in order, with no sort',
    'indexes.sql',
    $$SELECT dojo.plan($q$SELECT id, name, price FROM products WHERE attrs ->> 'brand' = 'Brand 07'
                         ORDER BY price, id LIMIT 10$q$) !~ '(Seq Scan|Sort)'$$,
    'the plan of the brand page still scans the table or sorts: index the expression attrs ->> ''brand'' followed by every ORDER BY column'
);

SELECT dojo.script_ok(
    'creates at most two indexes',
    'indexes.sql',
    $$SELECT count(*) <= 4 FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'products'$$,
    'products has more than two new indexes: every index slows down each write, so two must serve both queries'
);

SELECT dojo.script_eq(
    'the color search still returns its rows',
    'indexes.sql',
    $$SELECT id, sku FROM products WHERE attrs @> '{"color": "red"}' ORDER BY id$$,
    $$SELECT id, sku FROM products WHERE id % 97 = 0 ORDER BY id$$,
    ordered => true
);

-- reserve.sql
SELECT dojo.script_eq(
    'a reservation adds to reserved and keeps on_hand',
    'reserve.sql',
    $$SELECT on_hand, reserved FROM stock WHERE product_id = 42$$,
    $$VALUES (5, 4)$$
);

SELECT dojo.script_ok(
    'a reservation touches only product 42',
    'reserve.sql',
    $$SELECT count(*) = 0 FROM stock WHERE product_id <> 42 AND reserved <> product_id % 3$$,
    'another product''s stock changed: filter on product_id = 42'
);

SELECT dojo.query_eq(
    'reserves nothing when only 1 unit is left',
    'reserve.sql',
    $$SELECT 1, 1 WHERE false$$,
    setup => $$UPDATE stock SET reserved = 4 WHERE product_id = 42$$
);

SELECT dojo.script_eq(
    'leaves the stock alone when there are not enough units',
    'reserve.sql',
    $$SELECT on_hand, reserved FROM stock WHERE product_id = 42$$,
    $$VALUES (5, 4)$$,
    setup => $$UPDATE stock SET reserved = 4 WHERE product_id = 42$$
);

SELECT dojo.query_eq(
    'reserves the last 2 units',
    'reserve.sql',
    $$VALUES (42, 0)$$,
    setup => $$UPDATE stock SET reserved = 3 WHERE product_id = 42$$
);

-- The race: session a reserves 2 of the 3 units and holds its transaction open; session b runs
-- the learner's statement, which must wait for a; then a commits. Only 1 unit is left, so b must
-- reserve nothing. Both sessions are dblink connections; what a committed is undone afterwards.
CREATE EXTENSION IF NOT EXISTS dblink;
CREATE FUNCTION pg_temp.race(file text) RETURNS text LANGUAGE plpgsql AS $$
DECLARE
    learner text;
    b_pid integer;
    waited boolean := false;
    a_committed boolean := false;
    rows_back integer;
    reserved_seen integer;
    failure text;
BEGIN
    PERFORM dblink_disconnect(c) FROM unnest(coalesce(dblink_get_connections(), '{}')) AS c
    WHERE c IN ('a', 'b');
    PERFORM dblink_connect('a', 'host=/tmp dbname=dojo user=dojo');
    PERFORM dblink_connect('b', 'host=/tmp dbname=dojo user=dojo');
    BEGIN
        learner := dojo.sql_of(file);
        SELECT pid INTO b_pid FROM dblink('b', 'SELECT pg_backend_pid()') AS t(pid integer);
        PERFORM dblink_exec('a', 'BEGIN');
        PERFORM dblink_exec('a', 'UPDATE stock SET reserved = reserved + 2 WHERE product_id = 42');
        PERFORM dblink_exec('b', 'BEGIN');
        PERFORM dblink_send_query('b', learner);
        FOR i IN 1 .. 50 LOOP
            IF cardinality(pg_blocking_pids(b_pid)) > 0 THEN
                waited := true;
                EXIT;
            END IF;
            PERFORM pg_sleep(0.1);
        END LOOP;
        PERFORM dblink_exec('a', 'COMMIT');
        a_committed := true;
        IF NOT waited THEN
            failure := 'reserve.sql did not wait for the other session''s lock on product 42: it must UPDATE that row';
        END IF;
        SELECT count(*) INTO rows_back
        FROM dblink_get_result('b', false) AS t(product_id integer, available integer);
        PERFORM * FROM dblink_get_result('b', false) AS t(product_id integer, available integer);
        IF dblink_error_message('b') <> 'OK' THEN
            failure := coalesce(failure, 'reserve.sql failed after the other session committed: '
                                         || dblink_error_message('b'));
        END IF;
        SELECT r INTO reserved_seen
        FROM dblink('b', 'SELECT reserved FROM stock WHERE product_id = 42', false) AS t(r integer);
        IF failure IS NULL AND (rows_back <> 0 OR reserved_seen IS DISTINCT FROM 4) THEN
            failure := format('after the other session took 2 of the 3 units, your statement returned %s row(s) and left reserved = %s (expected no row and 4): test the availability in the UPDATE''s WHERE, not in an earlier read',
                              rows_back, coalesce(reserved_seen::text, 'unknown'));
        END IF;
    EXCEPTION WHEN OTHERS THEN
        IF SQLERRM LIKE 'todo:%' OR SQLERRM LIKE 'the file % is missing%' THEN
            failure := SQLERRM;
        ELSE
            failure := coalesce(failure, 'the race test failed: ' || SQLERRM);
        END IF;
    END;
    PERFORM dblink_exec('b', 'ROLLBACK', false);
    IF a_committed THEN
        PERFORM dblink_exec('a', 'UPDATE stock SET reserved = reserved - 2 WHERE product_id = 42', false);
    ELSE
        PERFORM dblink_exec('a', 'ROLLBACK', false);
    END IF;
    PERFORM dblink_disconnect('a');
    PERFORM dblink_disconnect('b');
    RETURN failure;
END
$$;

SELECT dojo.record('decides on the stock as it is after a concurrent reservation commits',
                   pg_temp.race('reserve.sql'));

-- update_attrs.sql
SELECT dojo.query_eq(
    'saves the change and returns the new version',
    'update_attrs.sql',
    $$VALUES (42, 4)$$
);

SELECT dojo.script_ok(
    'changes the color, removes promo and keeps the other keys',
    'update_attrs.sql',
    $$SELECT attrs = '{"brand": "Brand 42", "color": "blue", "sizes": ["S", "M"]}'::jsonb
      FROM products WHERE id = 42$$,
    'product 42''s attrs should be {"brand": "Brand 42", "color": "blue", "sizes": ["S", "M"]}: merge with || and remove a key with -, instead of replacing the document'
);

SELECT dojo.query_eq(
    'saves nothing when someone saved the product in between',
    'update_attrs.sql',
    $$SELECT 1, 1 WHERE false$$,
    setup => $$UPDATE products SET version = 4 WHERE id = 42$$
);

SELECT dojo.script_ok(
    'leaves a product saved in between untouched',
    'update_attrs.sql',
    $$SELECT attrs ->> 'color' = 'green' AND attrs ? 'promo' AND version = 4 FROM products WHERE id = 42$$,
    'product 42 was at version 4, so the admin''s change (made on version 3) must not be saved',
    setup => $$UPDATE products SET version = 4 WHERE id = 42$$
);

-- units_by_product.sql
SELECT dojo.query_eq(
    'uses the price paid, not today''s catalogue price',
    'units_by_product.sql',
    $$VALUES ('SKU-00007', 'Product 7', 4, 20.50), ('SKU-00042', 'Product 42', 3, 60.80),
             ('SKU-00100', 'Product 100', 3, 6.00), ('SKU-99999', NULL, 1, 12.00)$$,
    ordered => true,
    setup => $$UPDATE products SET price = 999 WHERE id IN (7, 42, 100)$$
);

SELECT dojo.query_eq(
    'counts the last second of November and keeps unknown skus',
    'units_by_product.sql',
    $$VALUES ('SKU-00042', 'Product 42', 1, 21.00), ('SKU-99999', NULL, 1, 12.00)$$,
    ordered => true,
    setup => $$DELETE FROM orders WHERE id <> 504$$
);

SELECT dojo.query_eq(
    'leaves out October and December',
    'units_by_product.sql',
    $$SELECT 'x', 'x', 1, 1 WHERE false$$,
    setup => $$DELETE FROM orders WHERE id NOT IN (503, 505, 506)$$
);
