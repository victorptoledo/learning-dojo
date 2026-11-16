SELECT d.year, d.month, p.category, sum(f.revenue) AS revenue
FROM fact_sales AS f
JOIN dim_date AS d USING (date_key)
JOIN dim_product AS p USING (product_key)
GROUP BY d.year, d.month, p.category
ORDER BY d.year, d.month, p.category;
