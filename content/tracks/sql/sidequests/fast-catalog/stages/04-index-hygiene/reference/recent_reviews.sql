SELECT id, rating, created_at
FROM reviews
WHERE product_id = 65413
  AND rating >= 4
ORDER BY created_at DESC, id DESC
LIMIT 5;
