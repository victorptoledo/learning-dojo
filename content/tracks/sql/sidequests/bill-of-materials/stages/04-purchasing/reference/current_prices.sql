-- First the price each supplier charges on the day (its latest row up to that day), then the
-- cheapest supplier of each part: two window functions over two different groupings.
WITH valid AS (
    SELECT part_id, supplier_id, price,
           row_number() OVER (PARTITION BY part_id, supplier_id ORDER BY valid_from DESC) AS newest
    FROM prices
    WHERE valid_from <= date '2026-11-01'
),
ranked AS (
    SELECT v.part_id, s.name AS supplier, v.price,
           row_number() OVER (PARTITION BY v.part_id ORDER BY v.price, s.name) AS position
    FROM valid v
    JOIN suppliers s ON s.id = v.supplier_id
    WHERE v.newest = 1
)
SELECT p.sku, r.supplier, r.price
FROM ranked r
JOIN parts p ON p.id = r.part_id
WHERE r.position = 1
ORDER BY p.sku;
