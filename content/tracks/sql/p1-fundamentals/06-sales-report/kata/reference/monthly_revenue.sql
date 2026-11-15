SELECT date_trunc('month', o.placed_at)::date AS month,
       count(DISTINCT o.id) AS paid_orders,
       sum(i.quantity * i.unit_price) AS revenue
FROM orders AS o
JOIN order_items AS i ON i.order_id = o.id
WHERE o.status = 'paid'
  AND o.placed_at >= TIMESTAMPTZ '2026-01-01 00:00+00'
  AND o.placed_at < TIMESTAMPTZ '2027-01-01 00:00+00'
GROUP BY date_trunc('month', o.placed_at)
HAVING sum(i.quantity * i.unit_price) >= 100
ORDER BY month;
