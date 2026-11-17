SELECT id, sku, created_at
FROM products
WHERE active
ORDER BY created_at DESC, id DESC
LIMIT 25;
