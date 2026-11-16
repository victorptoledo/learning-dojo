-- Dropped columns stay in pg_attribute (attisdropped) until the table is rewritten, and
-- system columns such as xmin and ctid have attnum < 0.
SELECT attname AS column_name,
       format_type(atttypid, atttypmod) AS data_type,
       attnotnull AS not_null
FROM pg_attribute
WHERE attrelid = 'shop.orders'::regclass
  AND attnum > 0
  AND NOT attisdropped
ORDER BY attnum;
