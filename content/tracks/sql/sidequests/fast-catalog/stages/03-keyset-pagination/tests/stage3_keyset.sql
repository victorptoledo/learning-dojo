-- Stage 3 contract: newest.sql and next_page.sql page through the newest products without
-- OFFSET, at the same cost on the first page and on the last.

SELECT dojo.query_eq(
    'the first page holds the 25 newest active products',
    'newest.sql',
    $$SELECT id, sku, created_at FROM products WHERE active
      ORDER BY created_at DESC, id DESC LIMIT 25$$,
    ordered => true
);

SELECT dojo.query_eq(
    'the next page starts right after the cursor',
    'next_page.sql',
    $$SELECT id, sku, created_at FROM products WHERE active
      ORDER BY created_at DESC, id DESC OFFSET 25 LIMIT 25$$,
    ordered => true,
    setup => $$UPDATE page_cursor SET (created_at, id) =
                    (SELECT created_at, id FROM products WHERE active
                     ORDER BY created_at DESC, id DESC OFFSET 24 LIMIT 1)$$
);

SELECT dojo.query_eq(
    'rows with the same time are split by id',
    'next_page.sql',
    $$SELECT id, sku, created_at FROM products
      WHERE active AND (created_at < (SELECT created_at FROM products WHERE id = 3001)
                        OR (created_at = (SELECT created_at FROM products WHERE id = 3001) AND id < 3001))
      ORDER BY created_at DESC, id DESC LIMIT 25$$,
    ordered => true,
    setup => $$UPDATE page_cursor SET (created_at, id) = (SELECT created_at, id FROM products WHERE id = 3001)$$
);

SELECT dojo.query_eq(
    'the last page is short',
    'next_page.sql',
    $$SELECT id, sku, created_at FROM products WHERE active AND id < 12
      ORDER BY created_at DESC, id DESC$$,
    ordered => true,
    setup => $$UPDATE page_cursor SET (created_at, id) = (SELECT created_at, id FROM products WHERE id = 12)$$
);

SELECT dojo.script_ok(
    'pages are found by their keys, not by OFFSET',
    NULL,
    $$SELECT dojo.sql_of('next_page.sql') !~* '\moffset\M'$$,
    'next_page.sql must not use OFFSET: skipping rows still reads them all'
);

SELECT dojo.script_ok(
    'the first page reads at most 10 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('newest.sql', 10)$$,
    'newest.sql is over its budget'
);

SELECT dojo.script_ok(
    'a page deep down reads at most 12 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('next_page.sql', 12)$$,
    'next_page.sql is over its budget',
    setup => $$UPDATE page_cursor SET (created_at, id) = (SELECT created_at, id FROM products WHERE id = 900)$$
);

SELECT dojo.script_ok(
    'a page in the middle reads at most 12 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('next_page.sql', 12)$$,
    'next_page.sql is over its budget',
    setup => $$UPDATE page_cursor SET (created_at, id) = (SELECT created_at, id FROM products WHERE id = 50000)$$
);
