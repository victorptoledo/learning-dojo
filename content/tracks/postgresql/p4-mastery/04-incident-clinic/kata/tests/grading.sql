-- Other sessions, opened with dblink, play the rest of the application.
CREATE EXTENSION IF NOT EXISTS dblink;

CREATE FUNCTION pg_temp.session(app text, sql text) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF NOT app = ANY (coalesce(dblink_get_connections(), '{}')) THEN
        PERFORM dblink_connect(app, format('host=/tmp dbname=dojo user=dojo application_name=%s', app));
    END IF;
    PERFORM dblink_exec(app, sql);
END
$$;

CREATE FUNCTION pg_temp.close_sessions() RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    name text;
BEGIN
    FOREACH name IN ARRAY coalesce(dblink_get_connections(), '{}') LOOP
        PERFORM dblink_disconnect(name);
    END LOOP;
    FOR i IN 1 .. 100 LOOP
        PERFORM pg_stat_clear_snapshot();  -- pg_stat_activity is read once per transaction otherwise
        EXIT WHEN NOT EXISTS (SELECT 1 FROM pg_stat_activity
                              WHERE pid <> pg_backend_pid() AND backend_type = 'client backend');
        PERFORM pg_sleep(0.05);
    END LOOP;
END
$$;

-- vacuum_blockers.sql
-- reporting: a REPEATABLE READ report that read once and went quiet (it keeps its snapshot).
-- batch: changed a row and went quiet (it keeps its transaction id).
-- web: connected, no transaction. idle-tx: BEGIN only, nothing read yet.
SELECT pg_temp.session('reporting', 'BEGIN ISOLATION LEVEL REPEATABLE READ');
SELECT pg_temp.session('reporting', 'CREATE TEMP TABLE r AS SELECT count(*) FROM orders');
SELECT pg_temp.session('batch', 'BEGIN');
SELECT pg_temp.session('batch', 'UPDATE orders SET total = total + 1 WHERE id = 1');
SELECT pg_temp.session('web', 'SET search_path = public');
SELECT pg_temp.session('idle-tx', 'BEGIN');

SELECT dojo.query_eq(
    'finds the sessions that hold back vacuum',
    'vacuum_blockers.sql',
    $$VALUES ('reporting', 'idle in transaction'), ('batch', 'idle in transaction')$$
);
SELECT pg_temp.close_sessions();

SELECT pg_temp.session('web', 'SET search_path = public');
SELECT dojo.query_eq(
    'leaves out quiet sessions and your own',
    'vacuum_blockers.sql',
    $$SELECT 'x', 'y' WHERE false$$
);
SELECT pg_temp.close_sessions();

-- sequences.sql
SELECT dojo.query_eq(
    'finds the sequences running out, by type',
    'sequences.sql',
    $$VALUES ('public', 'orders_id_seq', 'integer', 95.0), ('public', 'tickets_id_seq', 'smallint', 91.6)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'counts a custom maximum and other schemas',
    'sequences.sql',
    $$VALUES ('billing', 'invoice_no', 'bigint', 99.0), ('public', 'orders_id_seq', 'integer', 95.0),
             ('public', 'tickets_id_seq', 'smallint', 91.6)$$,
    ordered => true,
    setup => $$CREATE SCHEMA billing;
                CREATE SEQUENCE billing.invoice_no MAXVALUE 1000;
                SELECT setval('billing.invoice_no', 990)$$
);

SELECT dojo.query_eq(
    'ignores sequences that were never used, and 75% exactly',
    'sequences.sql',
    $$VALUES ('public', 'orders_id_seq', 'integer', 95.0), ('public', 'tickets_id_seq', 'smallint', 91.6)$$,
    ordered => true,
    setup => $$CREATE SEQUENCE edge MAXVALUE 100; SELECT setval('edge', 75)$$
);

-- top_queries.sql
SELECT dojo.query_eq(
    'ranks the queries by total time',
    'top_queries.sql',
    $$VALUES (101, 1800, 50.0, 0.20, 98.9), (102, 960, 26.7, 800.00, 5.0), (104, 600, 16.7, 12.00, 100.0)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a query that never touched a page has no hit ratio',
    'top_queries.sql',
    $$VALUES (106, 5000, 58.8, 500000.00, NULL::numeric), (101, 1800, 21.2, 0.20, 98.9), (102, 960, 11.3, 800.00, 5.0)$$,
    ordered => true,
    setup => $$UPDATE statements_snapshot SET total_exec_time = 5000000 WHERE queryid = 106$$
);

-- fix_orders_id.sql
SELECT dojo.script_eq(
    'orders.id and its references are bigint',
    'fix_orders_id.sql',
    $$SELECT attrelid::regclass::text, attname, format_type(atttypid, atttypmod) FROM pg_attribute
      WHERE (attrelid, attname) IN (('orders'::regclass, 'id'), ('order_items'::regclass, 'order_id'))$$,
    $$VALUES ('orders', 'id', 'bigint'), ('order_items', 'order_id', 'bigint')$$
);

SELECT dojo.script_eq(
    'new orders get ids beyond the integer limit',
    'fix_orders_id.sql',
    $$SELECT id FROM orders WHERE customer = 'beyond'$$,
    $$VALUES (2147483648)$$,
    after => $$SELECT setval('orders_id_seq', 2147483647);
               INSERT INTO orders (customer, total) VALUES ('beyond', 1)$$
);

SELECT dojo.script_ok(
    'items can reference the new orders, and nothing was lost',
    'fix_orders_id.sql',
    $$SELECT (SELECT count(*) FROM orders) = 101 AND (SELECT count(*) FROM order_items) = 101$$,
    'the orders and their items must survive the change',
    after => $$SELECT setval('orders_id_seq', 2147483647);
               INSERT INTO orders (customer, total) VALUES ('beyond', 1);
               INSERT INTO order_items (order_id, line_no, sku) VALUES (2147483648, 1, 'SKU-2')$$
);
