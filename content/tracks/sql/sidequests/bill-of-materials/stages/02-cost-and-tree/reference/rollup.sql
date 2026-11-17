-- One walk per assembly: every assembly is a root, and the walk carries which root it came
-- from. The cost of a root is the cost of the components below it; an assembly with nothing
-- below it still shows, at 0.
WITH RECURSIVE tree (root_id, part_id, quantity) AS (
    SELECT b.parent_id, b.child_id, b.quantity
    FROM bom b
    JOIN parts p ON p.id = b.parent_id
    WHERE p.kind = 'assembly'
    UNION ALL
    SELECT t.root_id, b.child_id, t.quantity * b.quantity
    FROM tree t
    JOIN bom b ON b.parent_id = t.part_id
),
costs AS (
    SELECT t.root_id, sum(t.quantity * c.unit_cost) AS cost
    FROM tree t
    JOIN parts c ON c.id = t.part_id AND c.kind = 'component'
    GROUP BY t.root_id
)
SELECT a.sku, coalesce(costs.cost, 0) AS cost
FROM parts a
LEFT JOIN costs ON costs.root_id = a.id
WHERE a.kind = 'assembly'
ORDER BY cost DESC, a.sku;
