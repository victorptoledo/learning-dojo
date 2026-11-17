-- The same expression as the index, or the index cannot be used.
SELECT id, sku, name
FROM products
WHERE lower(sku) = lower('eh-004242');
