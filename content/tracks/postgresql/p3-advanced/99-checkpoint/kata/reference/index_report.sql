-- One row per index of the public schema that should go, with the first reason that applies.
WITH idx AS (
    SELECT i.indexrelid,
           i.indrelid,
           c.relname AS index_name,
           i.indisvalid,
           i.indisunique OR i.indisprimary AS is_unique,
           i.indpred IS NULL AND i.indexprs IS NULL AS plain,
           am.amname,
           string_to_array(i.indkey::text, ' ')::int[] AS cols
    FROM pg_index AS i
    JOIN pg_class AS c ON c.oid = i.indexrelid
    JOIN pg_namespace AS n ON n.oid = c.relnamespace
    JOIN pg_am AS am ON am.oid = c.relam
    WHERE n.nspname = 'public'
),
reasons AS (
    SELECT a.index_name,
           CASE
               WHEN NOT a.indisvalid THEN 'invalid'
               -- Its columns lead another valid plain B-tree of the same table, so that one
               -- serves every query it serves. Of two identical ones, keep the first by name.
               WHEN NOT a.is_unique AND a.plain AND a.amname = 'btree' AND EXISTS (
                   SELECT 1 FROM idx AS b
                   WHERE b.indrelid = a.indrelid
                     AND b.indexrelid <> a.indexrelid
                     AND b.indisvalid AND b.plain AND b.amname = 'btree'
                     AND b.cols[1:cardinality(a.cols)] = a.cols
                     AND (cardinality(b.cols) > cardinality(a.cols) OR b.index_name < a.index_name)
               ) THEN 'redundant'
               WHEN NOT a.is_unique AND u.idx_scan = 0 THEN 'unused'
           END AS reason
    FROM idx AS a
    LEFT JOIN index_usage AS u ON u.index_name = a.index_name
)
SELECT index_name, reason
FROM reasons
WHERE reason IS NOT NULL
ORDER BY index_name;
