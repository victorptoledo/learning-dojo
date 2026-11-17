-- Stage 2 contract: low_stock.sql through a small partial index, brand_summary.sql through
-- an index-only scan.

SELECT dojo.query_eq(
    'lists the active products running out',
    'low_stock.sql',
    $$SELECT id, sku, stock FROM products WHERE active AND stock < 5 ORDER BY stock, id$$,
    ordered => true
);

SELECT dojo.query_eq(
    'the low-stock list follows the stock',
    'low_stock.sql',
    $$SELECT id, sku, stock FROM products WHERE active AND stock < 5 ORDER BY stock, id$$,
    ordered => true,
    setup => $$SELECT catalog_test.private_copy('products');
                UPDATE products SET stock = 0 WHERE id BETWEEN 500 AND 520;
                UPDATE products SET active = false WHERE id IN (SELECT id FROM products WHERE stock = 1 LIMIT 10)$$
);

SELECT dojo.script_ok(
    'the low-stock list reads at most 30 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('low_stock.sql', 30)$$,
    'low_stock.sql is over its budget'
);

SELECT dojo.script_ok(
    'the low-stock list uses a small partial index',
    'indexes.sql',
    $$SELECT EXISTS (
          SELECT FROM pg_indexes i
          WHERE i.schemaname = 'public' AND i.tablename = 'products'
            AND i.indexdef LIKE '% WHERE %'
            AND pg_relation_size(quote_ident(i.indexname)::regclass) < 512 * 1024
            AND dojo.plan(dojo.sql_of('low_stock.sql')) LIKE '% ' || i.indexname || ' %')$$,
    'the plan of low_stock.sql should use a partial index (CREATE INDEX ... WHERE ...) of less than 512 kB: one that holds only the rows the page lists'
);

SELECT dojo.query_eq(
    'summarises the active products of a brand',
    'brand_summary.sql',
    $$SELECT count(*), min(price), max(price) FROM products WHERE brand = 'Kestrel' AND active$$
);

SELECT dojo.query_eq(
    'the summary follows the prices',
    'brand_summary.sql',
    $$SELECT count(*), min(price), max(price) FROM products WHERE brand = 'Kestrel' AND active$$,
    setup => $$SELECT catalog_test.private_copy('products');
                UPDATE products SET price = 1.00 WHERE id = (SELECT max(id) FROM products WHERE brand = 'Kestrel' AND NOT active);
                UPDATE products SET price = 999.99 WHERE id = (SELECT min(id) FROM products WHERE brand = 'Kestrel' AND active)$$
);

SELECT dojo.script_ok(
    'the brand summary reads at most 40 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('brand_summary.sql', 40)$$,
    'brand_summary.sql is over its budget'
);

SELECT dojo.script_ok(
    'the brand summary never visits the table',
    'indexes.sql',
    $$SELECT dojo.plan(dojo.sql_of('brand_summary.sql')) LIKE '%Index Only Scan%'$$,
    'the plan of brand_summary.sql should be an Index Only Scan: every column the query reads must be in the index (INCLUDE adds columns that are not search keys)'
);
