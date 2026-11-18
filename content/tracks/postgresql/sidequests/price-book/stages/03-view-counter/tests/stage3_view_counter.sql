-- Stage 3 contract: views.sql creates product_stats and record_view(); a second round of views
-- is updated in place (HOT), and two sessions viewing one product never lose a click.

-- 1,000 products, each viewed twice: the first round inserts, the second updates. Returns the
-- percentage of those updates that were HOT, as this transaction's counters see them.
CREATE FUNCTION pb_test.hot_percent() RETURNS numeric
LANGUAGE plpgsql AS $$
DECLARE
    updated bigint;
    hot bigint;
BEGIN
    INSERT INTO products (sku, name)
    SELECT format('HOT-%s', lpad(i::text, 4, '0')), 'Product ' || i FROM generate_series(1, 1000) AS i;
    PERFORM record_view(id) FROM products WHERE sku LIKE 'HOT-%' ORDER BY id;
    SELECT n_tup_upd, n_tup_hot_upd INTO updated, hot
    FROM pg_stat_xact_user_tables WHERE relid = 'product_stats'::regclass;
    PERFORM record_view(id) FROM products WHERE sku LIKE 'HOT-%' ORDER BY id;
    SELECT n_tup_upd - updated, n_tup_hot_upd - hot INTO updated, hot
    FROM pg_stat_xact_user_tables WHERE relid = 'product_stats'::regclass;
    IF updated < 1000 THEN
        RAISE EXCEPTION 'the second round of 1,000 views should update 1,000 rows of product_stats, it updated %', updated;
    END IF;
    RETURN round(100.0 * hot / updated, 1);
END
$$;

-- Session A views a product in an open transaction, session B views it too; A then ends with
-- first_ends. `existing` says whether the product already had 5 views (committed) or none.
CREATE FUNCTION pb_test.two_clicks(existing boolean, first_ends text) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    before integer := CASE WHEN existing THEN 5 ELSE 0 END;
    total bigint;
BEGIN
    PERFORM pb_test.reset();
    PERFORM pb_test.open('a', 'b');
    PERFORM dblink_exec('a', 'INSERT INTO products (sku, name) VALUES (''CLK-0001'', ''Clicked'')');
    IF existing THEN
        PERFORM pb_test.run('a', 'SELECT max(record_view(id)) FROM products, generate_series(1, 5) WHERE sku = ''CLK-0001''');
    END IF;
    PERFORM dblink_exec('a', 'BEGIN');
    a := pb_test.run('a', 'SELECT record_view(id) FROM products WHERE sku = ''CLK-0001''');
    PERFORM pb_test.expect(a = (before + 1)::text,
        format('session A''s view should return %s, got %s', before + 1, coalesce(a, 'NULL')));
    PERFORM pb_test.start('b', 'SELECT record_view(id) FROM products WHERE sku = ''CLK-0001''');
    b_waited := pb_test.waiting('b');
    PERFORM dblink_exec('a', first_ends);
    b := pb_test.finish('b');
    PERFORM pb_test.close_all();
    SELECT s.views INTO total FROM product_stats s JOIN products p ON p.id = s.product_id WHERE p.sku = 'CLK-0001';
    PERFORM pb_test.expect(b_waited,
        'session B should wait for session A''s open transaction, but it finished at once with ' || coalesce(b, 'NULL'));
    PERFORM pb_test.expect(b NOT LIKE 'error:%', 'session B''s view should not fail: ' || b);
    IF first_ends = 'COMMIT' THEN
        PERFORM pb_test.expect(b = (before + 2)::text and total = before + 2, format(
            'after A committed its view, B should return %s and the product have %s views; B returned %s and it has %s',
            before + 2, before + 2, coalesce(b, 'NULL'), coalesce(total::text, 'no row')));
    ELSE
        PERFORM pb_test.expect(b = (before + 1)::text and total = before + 1, format(
            'after A rolled back its view, B should return %s and the product have %s views; B returned %s and it has %s',
            before + 1, before + 1, coalesce(b, 'NULL'), coalesce(total::text, 'no row')));
    END IF;
    PERFORM pb_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM pb_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_eq(
    'record_view counts the views of a product',
    NULL,
    $$SELECT record_view(k) FROM (SELECT id AS k FROM products) AS p, generate_series(1, 3)$$,
    $$VALUES (1::bigint), (2), (3)$$,
    ordered => true,
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql');
               INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle')$$
);

SELECT dojo.script_ok(
    'record_view keeps one row per product',
    NULL,
    $$SELECT (SELECT array_agg(views ORDER BY views) FROM product_stats) = '{1,2}'
         AND (SELECT bool_and(last_viewed_at = now()) FROM product_stats)$$,
    'after 2 views of one product and 1 of another, product_stats should hold views 2 and 1, with last_viewed_at = now()',
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql');
               INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle'), ('MUG-0001', 'Mug')$$,
    after => $$SELECT record_view(id) FROM products;
               SELECT record_view(id) FROM products WHERE sku = 'MUG-0001'$$
);

SELECT dojo.script_fails('only products are viewed', NULL,
    $$SELECT record_view(-1)$$, '23503',
    'product -1 does not exist',
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql')$$);

SELECT dojo.script_ok(
    'a second round of views is HOT',
    NULL,
    $$SELECT pb_test.hot_percent() >= 90$$,
    'less than 90% of the second round of updates were HOT (see pb_test.hot_percent()): leave free space on every page (fillfactor) and index nothing the counter changes',
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql')$$
);

SELECT dojo.script_ok('two clicks at once both count', NULL,
    $$SELECT pb_test.two_clicks(true, 'COMMIT')$$, 'see above',
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql')$$);

SELECT dojo.script_ok('a rolled back click does not count', NULL,
    $$SELECT pb_test.two_clicks(true, 'ROLLBACK')$$, 'see above',
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql')$$);

SELECT dojo.script_ok('two first clicks at once both count', NULL,
    $$SELECT pb_test.two_clicks(false, 'COMMIT')$$, 'see above',
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql')$$);

SELECT dojo.script_ok('a rolled back first click leaves room for the next', NULL,
    $$SELECT pb_test.two_clicks(false, 'ROLLBACK')$$, 'see above',
    setup => $$SELECT pb_test.require('catalog.sql', 'views.sql')$$);
