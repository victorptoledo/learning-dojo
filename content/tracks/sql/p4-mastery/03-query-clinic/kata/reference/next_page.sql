-- Continue after the last row of the previous page; orders_placed_id seeks straight there.
SELECT id, placed_at, total
FROM orders
WHERE (placed_at, id) > (TIMESTAMPTZ '2026-02-18 23:00+00', 1500)
ORDER BY placed_at, id
LIMIT 20;
