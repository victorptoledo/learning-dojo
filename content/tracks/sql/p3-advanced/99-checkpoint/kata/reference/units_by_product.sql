-- Unnest the item arrays into typed rows, keep November with a half-open range, and LEFT JOIN
-- the catalogue so that unknown skus stay in the report.
SELECT i.sku,
       p.name,
       sum(i.qty) AS units,
       sum(i.qty * i.price) AS revenue
FROM orders o
CROSS JOIN LATERAL jsonb_to_recordset(o.items) AS i (sku text, qty integer, price numeric)
LEFT JOIN products p ON p.sku = i.sku
WHERE o.placed_at >= TIMESTAMPTZ '2026-11-01 00:00+00'
  AND o.placed_at <  TIMESTAMPTZ '2026-12-01 00:00+00'
GROUP BY i.sku, p.name
ORDER BY units DESC, i.sku;
