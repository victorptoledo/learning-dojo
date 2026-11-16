SELECT schemaname AS schema_name,
       sequencename AS sequence_name,
       data_type::text,
       round(100.0 * last_value / max_value, 1) AS percent_used
FROM pg_sequences
WHERE last_value IS NOT NULL
  AND last_value > 0.75 * max_value
ORDER BY percent_used DESC, sequence_name;
