SELECT c.country, d.year, d.quarter, count(DISTINCT f.order_id) AS orders,
       sum(f.revenue) AS revenue
FROM fact_sales AS f
JOIN dim_date AS d USING (date_key)
JOIN dim_customer AS c USING (customer_key)
GROUP BY c.country, d.year, d.quarter
ORDER BY c.country, d.year, d.quarter;
