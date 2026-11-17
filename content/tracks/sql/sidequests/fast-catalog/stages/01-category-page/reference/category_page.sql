SELECT id, sku, price
FROM products
WHERE category_id = 7
  AND active
ORDER BY price, id
LIMIT 20;
