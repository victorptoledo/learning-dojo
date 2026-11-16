SELECT n.nspname AS schema_name,
       c.relname AS table_name,
       pg_total_relation_size(c.oid) AS total_bytes,
       pg_indexes_size(c.oid) AS index_bytes
FROM pg_class AS c
JOIN pg_namespace AS n ON n.oid = c.relnamespace
WHERE c.relkind IN ('r', 'p')
  AND n.nspname IN ('shop', 'audit')
ORDER BY total_bytes DESC, schema_name, table_name;
