WITH ranked AS (
    SELECT order_id, status, amount,
           row_number() OVER (PARTITION BY order_id
                              ORDER BY updated_at DESC, _ingested_at DESC) AS rn,
           count(*) OVER (PARTITION BY order_id) AS raw_rows
    FROM raw_orders
)
SELECT order_id, status, amount, raw_rows
FROM ranked
WHERE rn = 1
ORDER BY order_id;
