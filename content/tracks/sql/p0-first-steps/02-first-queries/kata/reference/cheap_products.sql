SELECT name, price
FROM products
WHERE price < 20
  AND stock > 0
ORDER BY price, name;
