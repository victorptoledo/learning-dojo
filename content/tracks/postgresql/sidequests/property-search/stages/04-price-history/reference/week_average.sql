SELECT round(avg(price), 2) AS average_price, count(*) AS observations
FROM price_history
WHERE observed_at >= '2024-03-04' AND observed_at < '2024-03-11';
