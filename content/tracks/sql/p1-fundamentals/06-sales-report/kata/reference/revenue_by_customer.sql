SELECT c.name,
       count(DISTINCT o.id) AS paid_orders,
       coalesce(sum(i.quantity * i.unit_price), 0) AS revenue
FROM customers AS c
LEFT JOIN orders AS o
       ON o.customer_id = c.id
      AND o.status = 'paid'
LEFT JOIN order_items AS i ON i.order_id = o.id
GROUP BY c.id, c.name
ORDER BY revenue DESC, c.name;
