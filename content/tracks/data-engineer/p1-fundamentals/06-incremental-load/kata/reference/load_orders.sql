-- Incremental, idempotent load of orders from the landing table.
-- 1. Read only the rows that landed after the watermark.
-- 2. Keep the latest version of each order in that batch (newest updated_at, then the row
--    that landed last), so duplicates never reach the upsert.
-- 3. Upsert, but never replace a row with an older or equally old version.
-- 4. Move the watermark to the newest row read; an empty batch leaves it where it was.
WITH batch AS (
    SELECT r.*
    FROM raw_orders AS r
    JOIN load_state AS s ON s.table_name = 'orders'
    WHERE r._ingested_at > s.watermark
),
ranked AS (
    SELECT order_id, status, amount, updated_at,
           row_number() OVER (PARTITION BY order_id
                              ORDER BY updated_at DESC, _ingested_at DESC) AS rn
    FROM batch
)
INSERT INTO orders (order_id, status, amount, updated_at)
SELECT order_id, status, amount, updated_at
FROM ranked
WHERE rn = 1
ON CONFLICT (order_id) DO UPDATE
SET status = excluded.status,
    amount = excluded.amount,
    updated_at = excluded.updated_at
WHERE excluded.updated_at > orders.updated_at;

UPDATE load_state AS s
SET watermark = coalesce(
        (SELECT max(r._ingested_at) FROM raw_orders AS r WHERE r._ingested_at > s.watermark),
        s.watermark)
WHERE s.table_name = 'orders';
