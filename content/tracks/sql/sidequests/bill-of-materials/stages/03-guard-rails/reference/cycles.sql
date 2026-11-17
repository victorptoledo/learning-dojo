-- Walk down from every part. The CYCLE clause stops a path the moment it meets a part already
-- on it; a part that meets itself is in a cycle.
WITH RECURSIVE walk (start_id, part_id) AS (
    SELECT parent_id, child_id
    FROM bom
    UNION ALL
    SELECT w.start_id, b.child_id
    FROM walk w
    JOIN bom b ON b.parent_id = w.part_id
) CYCLE part_id SET is_cycle USING path
SELECT DISTINCT p.sku
FROM walk w
JOIN parts p ON p.id = w.start_id
WHERE w.part_id = w.start_id
ORDER BY p.sku;
