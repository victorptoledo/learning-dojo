-- most_common_vals is an anyarray: through text it becomes a text[]. unnest with two
-- arguments walks both arrays side by side.
SELECT mcv.value AS status, round(mcv.frequency::numeric, 3) AS frequency
FROM pg_stats AS s,
     unnest(s.most_common_vals::text::text[], s.most_common_freqs) AS mcv(value, frequency)
WHERE s.schemaname = 'public' AND s.tablename = 'orders' AND s.attname = 'status'
ORDER BY mcv.frequency DESC, mcv.value;
