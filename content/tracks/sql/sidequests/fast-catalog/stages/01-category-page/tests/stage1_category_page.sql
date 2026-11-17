-- Stage 1 contract: category_page.sql and sku_lookup.sql return the right rows, and with
-- indexes.sql they stay within their buffer budgets; skus are unique ignoring case.

SELECT dojo.query_eq(
    'the category page lists the 20 cheapest active products',
    'category_page.sql',
    $$SELECT id, sku, price FROM products WHERE category_id = 7 AND active
      ORDER BY price, id LIMIT 20$$,
    ordered => true
);

SELECT dojo.query_eq(
    'equal prices are ordered by id',
    'category_page.sql',
    $$SELECT id, sku, price FROM products WHERE category_id = 7 AND active
      ORDER BY price, id LIMIT 20$$,
    ordered => true,
    setup => $$SELECT catalog_test.private_copy('products');
                UPDATE products SET price = 5.00 WHERE category_id = 7 AND id % 50 = 0$$
);

SELECT dojo.query_eq(
    'finds a product by sku in any case',
    'sku_lookup.sql',
    $$VALUES (4242::bigint, 'EH-004242', 'Product 4242')$$
);

SELECT dojo.script_ok(
    'indexes.sql creates its indexes',
    'indexes.sql',
    $$SELECT count(*) > (SELECT count(*) FROM fixture_indexes)
      FROM pg_indexes WHERE schemaname = 'public'$$,
    'indexes.sql should create at least one index',
    setup => $$CREATE TEMP TABLE fixture_indexes AS SELECT indexname FROM pg_indexes WHERE schemaname = 'public'$$
);

SELECT dojo.script_ok(
    'the category page reads at most 40 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('category_page.sql', 40)$$,
    'category_page.sql is over its budget'
);

SELECT dojo.script_ok(
    'the category page needs no sort',
    'indexes.sql',
    $$SELECT dojo.plan(dojo.sql_of('category_page.sql')) NOT LIKE '%Sort%'$$,
    'the plan of category_page.sql still sorts: an index can hand the rows over already in price order'
);

SELECT dojo.script_ok(
    'a sku lookup reads at most 6 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('sku_lookup.sql', 6)$$,
    'sku_lookup.sql is over its budget'
);

SELECT dojo.script_fails(
    'a sku cannot be repeated in another case',
    'indexes.sql',
    $$INSERT INTO products VALUES (100001, 'eh-004242', 'Copy', 1, 'Acme', 10, 10, true, now())$$,
    '23505',
    'EH-004242 exists, so eh-004242 must be refused: make the index on the lowercase sku UNIQUE'
);

SELECT dojo.script_ok(
    'a new sku is still accepted',
    'indexes.sql',
    $$SELECT count(*) = 1 FROM products WHERE id = 100001$$,
    'a product with a new sku should insert',
    after => $$INSERT INTO products VALUES (100001, 'ZZ-100001', 'New', 1, 'Acme', 10, 10, true, now())$$
);
