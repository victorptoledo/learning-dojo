SELECT count(*) AS products, min(price) AS cheapest, max(price) AS dearest
FROM products
WHERE brand = 'Kestrel'
  AND active;
