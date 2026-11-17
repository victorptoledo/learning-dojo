-- Stage 4 contract: recent_reviews.sql and review_summary.sql through an index on the
-- foreign key, and cleanup.sql drops the fixture's useless indexes without slowing anything.

SELECT dojo.query_eq(
    'lists the latest good reviews of a product',
    'recent_reviews.sql',
    $$SELECT id, rating, created_at FROM reviews WHERE product_id = 65413 AND rating >= 4
      ORDER BY created_at DESC, id DESC LIMIT 5$$,
    ordered => true
);

SELECT dojo.query_eq(
    'the latest reviews follow new ones',
    'recent_reviews.sql',
    $$SELECT id, rating, created_at FROM reviews WHERE product_id = 65413 AND rating >= 4
      ORDER BY created_at DESC, id DESC LIMIT 5$$,
    ordered => true,
    setup => $$SELECT catalog_test.private_copy('reviews');
                INSERT INTO reviews VALUES (200001, 65413, 5, '2026-01-01'), (200002, 65413, 5, '2026-01-01'),
                                           (200003, 65413, 3, '2026-02-01')$$
);

SELECT dojo.query_eq(
    'summarises the reviews of a product',
    'review_summary.sql',
    $$SELECT p.sku, count(r.id), round(avg(r.rating), 2)
      FROM products p LEFT JOIN reviews r ON r.product_id = p.id
      WHERE p.id = 65413 GROUP BY p.id, p.sku$$
);

SELECT dojo.query_eq(
    'a product without reviews has a count of 0',
    'review_summary.sql',
    $$SELECT sku, 0, NULL::numeric FROM products WHERE id = 65413$$,
    setup => $$SELECT catalog_test.private_copy('reviews');
                DELETE FROM reviews WHERE product_id = 65413$$
);

SELECT dojo.script_ok(
    'the latest reviews read at most 20 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('recent_reviews.sql', 20)$$,
    'recent_reviews.sql is over its budget'
);

SELECT dojo.script_ok(
    'the review summary reads at most 20 buffers',
    'indexes.sql',
    $$SELECT catalog_test.within('review_summary.sql', 20)$$,
    'review_summary.sql is over its budget'
);

SELECT dojo.script_ok(
    'cleanup drops the four useless indexes',
    'indexes.sql',
    $$SELECT NOT EXISTS (
          SELECT FROM pg_indexes
          WHERE schemaname = 'public'
            AND indexname IN ('products_id_idx', 'products_category_idx', 'products_active_idx',
                              'reviews_rating_idx'))$$,
    'after cleanup.sql, products_id_idx, products_category_idx, products_active_idx and reviews_rating_idx should be gone',
    after => $$SELECT dojo.run_file('cleanup.sql')$$
);

SELECT dojo.script_ok(
    'cleanup keeps the primary keys',
    'indexes.sql',
    $$SELECT count(*) = 3 FROM pg_indexes
      WHERE schemaname = 'public' AND indexname IN ('categories_pkey', 'products_pkey', 'reviews_pkey')$$,
    'the primary keys and their indexes must stay',
    after => $$SELECT dojo.run_file('cleanup.sql')$$
);

SELECT dojo.script_ok(
    'after the cleanup every page is still within budget',
    'indexes.sql',
    $$SELECT catalog_test.within('category_page.sql', 40) AND catalog_test.within('sku_lookup.sql', 6)
         AND catalog_test.within('low_stock.sql', 30) AND catalog_test.within('brand_summary.sql', 40)
         AND catalog_test.within('newest.sql', 10) AND catalog_test.within('recent_reviews.sql', 20)
         AND catalog_test.within('review_summary.sql', 20)$$,
    'a query went over its budget after cleanup.sql',
    after => $$SELECT dojo.run_file('cleanup.sql')$$
);

SELECT dojo.script_ok(
    'the catalogue keeps a lean set of indexes',
    'indexes.sql',
    $$SELECT count(*) <= 7 FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'products'$$,
    'products should end with at most 7 indexes (the primary key included): every index slows each insert and update',
    after => $$SELECT dojo.run_file('cleanup.sql')$$
);
