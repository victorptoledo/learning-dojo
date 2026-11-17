-- Depth first: the path of skus from the bike down to a part sorts every part right after
-- its parent and its siblings by sku.
WITH RECURSIVE tree (part_id, quantity, depth, path) AS (
    SELECT id, 1, 0, ARRAY[sku]
    FROM parts
    WHERE sku = 'BK-ROAD'
    UNION ALL
    SELECT b.child_id, b.quantity, t.depth + 1, t.path || p.sku
    FROM tree t
    JOIN bom b ON b.parent_id = t.part_id
    JOIN parts p ON p.id = b.child_id
)
SELECT t.depth, repeat('  ', t.depth) || p.sku || ' x' || t.quantity AS line
FROM tree t
JOIN parts p ON p.id = t.part_id
ORDER BY t.path;
