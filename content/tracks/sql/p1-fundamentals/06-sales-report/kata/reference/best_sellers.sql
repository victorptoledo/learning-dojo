SELECT p.name, sum(i.quantity) AS units
FROM order_items AS i
JOIN orders AS o ON o.id = i.order_id
JOIN products AS p ON p.id = i.product_id
WHERE o.status = 'paid'
GROUP BY p.id, p.name
ORDER BY units DESC, p.name
FETCH FIRST 3 ROWS ONLY;
