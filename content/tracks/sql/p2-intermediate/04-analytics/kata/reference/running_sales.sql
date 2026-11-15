SELECT day,
       revenue,
       sum(revenue) OVER (PARTITION BY date_trunc('month', day) ORDER BY day
                          ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS month_to_date,
       revenue - lag(revenue) OVER (ORDER BY day) AS change,
       round(avg(revenue) OVER (ORDER BY day ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 2) AS moving_average
FROM daily_sales
ORDER BY day;
