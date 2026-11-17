-- Same predicate as the partial index, so the planner can prove the index covers it.
SELECT id, sku, stock
FROM products
WHERE active
  AND stock < 5
ORDER BY stock, id;
