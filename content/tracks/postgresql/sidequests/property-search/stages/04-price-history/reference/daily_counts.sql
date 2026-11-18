SELECT observed_at::date AS day, count(*) AS observations, max(price) AS highest
FROM price_history
WHERE observed_at >= '2024-03-01' AND observed_at < '2024-04-01'
GROUP BY observed_at::date
ORDER BY day;
