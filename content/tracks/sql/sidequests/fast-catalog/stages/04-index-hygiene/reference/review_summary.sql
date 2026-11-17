-- The aggregate runs on the few reviews of one product, found through the index.
SELECT p.sku, count(r.id) AS reviews, round(avg(r.rating), 2) AS average
FROM products p
LEFT JOIN reviews r ON r.product_id = p.id
WHERE p.id = 65413
GROUP BY p.id, p.sku;
