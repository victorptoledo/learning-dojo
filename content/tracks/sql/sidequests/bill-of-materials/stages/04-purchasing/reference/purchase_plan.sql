-- Explode the bike, subtract the stock, price each line with the cheapest current supplier,
-- and keep a running total from the biggest line down.
WITH RECURSIVE tree (part_id, quantity) AS (
    SELECT b.child_id, b.quantity
    FROM bom b
    JOIN parts p ON p.id = b.parent_id
    WHERE p.sku = 'BK-ROAD'
    UNION ALL
    SELECT b.child_id, t.quantity * b.quantity
    FROM tree t
    JOIN bom b ON b.parent_id = t.part_id
),
needed AS (
    SELECT t.part_id, 20 * sum(t.quantity) - coalesce(min(s.on_hand), 0) AS to_buy
    FROM tree t
    JOIN parts p ON p.id = t.part_id AND p.kind = 'component'
    LEFT JOIN stock s ON s.part_id = t.part_id
    GROUP BY t.part_id
),
valid AS (
    SELECT part_id, supplier_id, price,
           row_number() OVER (PARTITION BY part_id, supplier_id ORDER BY valid_from DESC) AS newest
    FROM prices
    WHERE valid_from <= date '2026-11-01'
),
best AS (
    SELECT v.part_id, s.name AS supplier, v.price,
           row_number() OVER (PARTITION BY v.part_id ORDER BY v.price, s.name) AS position
    FROM valid v
    JOIN suppliers s ON s.id = v.supplier_id
    WHERE v.newest = 1
),
lines AS (
    SELECT p.sku, n.to_buy, b.supplier, b.price, n.to_buy * b.price AS line_cost
    FROM needed n
    JOIN parts p ON p.id = n.part_id
    LEFT JOIN best b ON b.part_id = n.part_id AND b.position = 1
    WHERE n.to_buy > 0
)
SELECT sku, to_buy, supplier, price, line_cost,
       sum(line_cost) OVER (ORDER BY line_cost DESC NULLS LAST, sku) AS running_total
FROM lines
ORDER BY line_cost DESC NULLS LAST, sku;
