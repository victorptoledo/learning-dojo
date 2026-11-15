-- Aggregate each child table on its own, then join: no row multiplies another.
WITH revenue AS (
    SELECT customer_id, sum(total) AS revenue
    FROM orders
    GROUP BY customer_id
),
ticket_counts AS (
    SELECT customer_id, count(*) AS tickets
    FROM tickets
    GROUP BY customer_id
)
SELECT c.name,
       coalesce(r.revenue, 0) AS revenue,
       coalesce(t.tickets, 0) AS tickets
FROM customers AS c
LEFT JOIN revenue AS r ON r.customer_id = c.id
LEFT JOIN ticket_counts AS t ON t.customer_id = c.id
ORDER BY c.name;
