-- Stage 4 contract: week_average.sql and daily_counts.sql return the right rows and stay
-- within their buffer budgets, with indexes on price_history of at most 256 kB in all.

SELECT dojo.query_eq(
    'the average of a week',
    'week_average.sql',
    $$SELECT round(avg(price), 2), count(*) FROM price_history
      WHERE observed_at >= '2024-03-04' AND observed_at < '2024-03-11'$$
);

SELECT dojo.query_eq(
    'observations per day of March 2024',
    'daily_counts.sql',
    $$SELECT (observed_at AT TIME ZONE 'UTC')::date, count(*), max(price) FROM price_history
      WHERE observed_at >= '2024-03-01' AND observed_at < '2024-04-01'
      GROUP BY 1 ORDER BY 1$$,
    ordered => true
);

SELECT dojo.script_ok(
    'the week reads at most 300 buffers',
    NULL,
    $$SELECT search_test.within('week_average.sql', 300)$$,
    'week_average.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'March reads at most 400 buffers',
    NULL,
    $$SELECT search_test.within('daily_counts.sql', 400)$$,
    'daily_counts.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'the indexes of price_history take at most 256 kB',
    NULL,
    $$SELECT coalesce(sum(pg_relation_size(indexrelid)), 0) <= 256 * 1024
      FROM pg_index WHERE indrelid = 'price_history'::regclass$$,
    'the indexes of price_history take more than 256 kB: a time window of an append-only log needs a far smaller index than a B-tree',
    setup => $$SELECT search_test.require('indexes.sql')$$
);
