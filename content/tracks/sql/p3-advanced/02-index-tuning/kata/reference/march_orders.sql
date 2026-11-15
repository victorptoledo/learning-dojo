SELECT count(*) AS orders, sum(total) AS revenue
FROM orders
WHERE placed_at >= TIMESTAMPTZ '2025-03-01 00:00+00'
  AND placed_at < TIMESTAMPTZ '2025-04-01 00:00+00'
  AND status = 'paid';
