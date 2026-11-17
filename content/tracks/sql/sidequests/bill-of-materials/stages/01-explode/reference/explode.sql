-- Walk down from the road bike, multiplying the quantities along each path; a component
-- reached by several paths (the bearings) adds them up.
WITH RECURSIVE tree (part_id, quantity) AS (
    SELECT b.child_id, b.quantity
    FROM bom b
    JOIN parts p ON p.id = b.parent_id
    WHERE p.sku = 'BK-ROAD'
    UNION ALL
    SELECT b.child_id, t.quantity * b.quantity
    FROM tree t
    JOIN bom b ON b.parent_id = t.part_id
)
SELECT p.sku, p.name, sum(t.quantity) AS quantity
FROM tree t
JOIN parts p ON p.id = t.part_id
WHERE p.kind = 'component'
GROUP BY p.sku, p.name
ORDER BY p.sku;
