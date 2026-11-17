-- Walk up from the bearing: each step finds the parents of the parts found so far.
WITH RECURSIVE users (part_id, level) AS (
    SELECT b.parent_id, 1
    FROM bom b
    JOIN parts p ON p.id = b.child_id
    WHERE p.sku = 'BEARING'
    UNION ALL
    SELECT b.parent_id, u.level + 1
    FROM users u
    JOIN bom b ON b.child_id = u.part_id
)
SELECT p.sku, min(u.level) AS level
FROM users u
JOIN parts p ON p.id = u.part_id
GROUP BY p.sku
ORDER BY level, p.sku;
