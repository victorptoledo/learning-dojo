-- The expected rows of the size and settings questions are computed from the catalogs at test
-- time, so they hold whatever the exact page counts are.

SELECT dojo.query_eq(
    'lists every table of shop and audit, largest first',
    'largest_tables.sql',
    $$VALUES ('shop', 'order_items', 360448, 147456),
             ('audit', 'log', 278528, 65536),
             ('shop', 'orders', 212992, 106496),
             ('shop', 'reviews', 131072, 49152),
             ('shop', 'shipments', 131072, 65536),
             ('shop', 'customers', 90112, 32768),
             ('shop', 'products', 49152, 32768)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'counts TOAST and indexes in the total size',
    'largest_tables.sql',
    $$SELECT n.nspname, c.relname, pg_total_relation_size(c.oid), pg_indexes_size(c.oid)
      FROM pg_class AS c JOIN pg_namespace AS n ON n.oid = c.relnamespace
      WHERE c.relkind IN ('r', 'p') AND n.nspname IN ('shop', 'audit')
      ORDER BY 3 DESC, 1, 2$$,
    ordered => true,
    setup => $$INSERT INTO shop.reviews (product_id, customer_id, stars, body)
                SELECT 1, 1, 5, repeat(md5(i::text), 200) FROM generate_series(1, 500) AS i$$
);

SELECT dojo.query_eq(
    'includes partitioned tables and skips other schemas, views and indexes',
    'largest_tables.sql',
    $$SELECT n.nspname, c.relname, pg_total_relation_size(c.oid), pg_indexes_size(c.oid)
      FROM pg_class AS c JOIN pg_namespace AS n ON n.oid = c.relnamespace
      WHERE c.relkind IN ('r', 'p') AND n.nspname IN ('shop', 'audit')
      ORDER BY 3 DESC, 1, 2$$,
    ordered => true,
    setup => $$CREATE TABLE audit.events (at timestamptz NOT NULL) PARTITION BY RANGE (at);
                CREATE TABLE audit.events_2026 PARTITION OF audit.events
                    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
                CREATE VIEW shop.paid_orders AS SELECT * FROM shop.orders WHERE status = 'paid';
                CREATE TABLE public.scratch AS SELECT generate_series(1, 50000) AS n$$
);

SELECT dojo.query_eq(
    'skips dropped columns and shows new ones last',
    'columns.sql',
    $$VALUES ('id', 'bigint', true),
             ('customer_id', 'bigint', true),
             ('status', 'text', true),
             ('placed_at', 'timestamp with time zone', true),
             ('total', 'numeric(12,2)', false),
             ('coupon', 'character varying(20)', false)$$,
    ordered => true,
    setup => $$ALTER TABLE shop.orders DROP COLUMN notes;
                ALTER TABLE shop.orders ADD COLUMN coupon varchar(20)$$
);

SELECT dojo.query_eq(
    'reads the table shop.orders, not another orders',
    'columns.sql',
    $$VALUES ('id', 'bigint', true),
             ('customer_id', 'bigint', true),
             ('status', 'text', true),
             ('placed_at', 'timestamp with time zone', true),
             ('total', 'numeric(12,2)', false),
             ('notes', 'text', false)$$,
    ordered => true,
    setup => $$CREATE TABLE public.orders (code text PRIMARY KEY)$$
);

SELECT dojo.query_eq(
    'finds the foreign keys without an index',
    'unindexed_fks.sql',
    $$VALUES ('shop.order_items', 'order_items_product_id_fkey', 'product_id'),
             ('shop.reviews', 'reviews_product_id_fkey', 'product_id')$$
);

SELECT dojo.query_eq(
    'an index that starts with the key columns covers it',
    'unindexed_fks.sql',
    $$VALUES ('shop.reviews', 'reviews_product_id_fkey', 'product_id')$$,
    setup => $$CREATE INDEX ON shop.order_items (product_id, quantity)$$
);

SELECT dojo.query_eq(
    'a key column that is not leading does not count',
    'unindexed_fks.sql',
    $$VALUES ('shop.order_items', 'order_items_product_id_fkey', 'product_id'),
             ('shop.reviews', 'reviews_product_id_fkey', 'product_id'),
             ('shop.shipments', 'shipments_order_id_line_no_fkey', 'order_id, line_no')$$,
    setup => $$DROP INDEX shop.shipments_line_order;
                CREATE INDEX ON shop.shipments (carrier, order_id, line_no)$$
);

SELECT dojo.query_eq(
    'half of a two-column key is not enough',
    'unindexed_fks.sql',
    $$VALUES ('shop.order_items', 'order_items_product_id_fkey', 'product_id'),
             ('shop.reviews', 'reviews_product_id_fkey', 'product_id'),
             ('shop.shipments', 'shipments_order_id_line_no_fkey', 'order_id, line_no')$$,
    setup => $$DROP INDEX shop.shipments_line_order;
                CREATE INDEX ON shop.shipments (order_id)$$
);

SELECT dojo.query_eq(
    'lists the settings that do not come from the defaults',
    'changed_settings.sql',
    $$SELECT name, setting, unit, source FROM pg_settings
      WHERE source NOT IN ('default', 'override') ORDER BY name$$,
    ordered => true
);

SELECT dojo.query_eq(
    'shows a setting changed in this session',
    'changed_settings.sql',
    $$SELECT name, setting, unit, source FROM pg_settings
      WHERE source NOT IN ('default', 'override') ORDER BY name$$,
    ordered => true,
    setup => $$SET LOCAL work_mem = '64MB'$$
);
