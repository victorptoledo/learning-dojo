-- lag() looks at the previous row of the same part and supplier, in date order.
SELECT p.sku,
       s.name AS supplier,
       x.valid_from,
       x.price,
       lag(x.price) OVER w AS previous_price,
       round(100 * (x.price - lag(x.price) OVER w) / lag(x.price) OVER w, 1) AS change_pct
FROM prices x
JOIN parts p ON p.id = x.part_id
JOIN suppliers s ON s.id = x.supplier_id
WINDOW w AS (PARTITION BY x.part_id, x.supplier_id ORDER BY x.valid_from)
ORDER BY p.sku, supplier, x.valid_from;
