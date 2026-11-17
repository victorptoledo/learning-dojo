-- One anti join or semi join per rule, glued with UNION ALL.
SELECT p.sku, 'assembly without parts' AS problem
FROM parts p
WHERE p.kind = 'assembly'
  AND NOT EXISTS (SELECT FROM bom b WHERE b.parent_id = p.id)
UNION ALL
SELECT p.sku, 'component with parts'
FROM parts p
WHERE p.kind = 'component'
  AND EXISTS (SELECT FROM bom b WHERE b.parent_id = p.id)
UNION ALL
SELECT p.sku, 'component without cost'
FROM parts p
WHERE p.kind = 'component'
  AND p.unit_cost IS NULL
UNION ALL
SELECT p.sku, 'component used nowhere'
FROM parts p
WHERE p.kind = 'component'
  AND NOT EXISTS (SELECT FROM bom b WHERE b.child_id = p.id)
UNION ALL
SELECT p.sku, 'assembly with a cost'
FROM parts p
WHERE p.kind = 'assembly'
  AND p.unit_cost IS NOT NULL
ORDER BY sku, problem;
