SELECT count(*) AS orders, sum(total) AS revenue
FROM orders
WHERE placed_at >= TIMESTAMPTZ '2026-03-01 00:00+00'
  AND placed_at < TIMESTAMPTZ '2026-04-01 00:00+00';
