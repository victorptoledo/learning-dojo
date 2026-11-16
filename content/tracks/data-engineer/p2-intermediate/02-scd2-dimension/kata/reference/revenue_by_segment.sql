-- Revenue by the segment the customer had on the day of each order (a point-in-time join).
SELECT coalesce(d.segment, 'unknown') AS segment, sum(f.amount) AS revenue
FROM fact_orders AS f
JOIN dim_customer AS d
  ON d.customer_id = f.customer_id
 AND f.order_date >= d.valid_from
 AND f.order_date < d.valid_to
GROUP BY coalesce(d.segment, 'unknown')
ORDER BY segment;
