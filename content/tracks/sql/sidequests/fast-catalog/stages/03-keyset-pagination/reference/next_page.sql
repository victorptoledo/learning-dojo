-- Keyset pagination: continue strictly after the last row shown, compared as a pair so equal
-- timestamps are split by id. The subquery is computed once, so the index walk starts there.
SELECT id, sku, created_at
FROM products
WHERE active
  AND (created_at, id) < (SELECT created_at, id FROM page_cursor)
ORDER BY created_at DESC, id DESC
LIMIT 25;
