-- A foreign key is covered when some index on the referencing table starts with exactly its
-- columns, in any order. indkey is an int2vector; through text it becomes a 1-based array.
SELECT c.conrelid::regclass::text AS table_name,
       c.conname AS constraint_name,
       (SELECT string_agg(a.attname, ', ' ORDER BY k.ord)
        FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
        JOIN pg_attribute AS a ON a.attrelid = c.conrelid AND a.attnum = k.attnum) AS columns
FROM pg_constraint AS c
WHERE c.contype = 'f'
  AND c.connamespace = 'shop'::regnamespace
  AND NOT EXISTS (
      SELECT 1
      FROM pg_index AS i,
           LATERAL (SELECT (string_to_array(i.indkey::text, ' ')::smallint[])[1:cardinality(c.conkey)] AS leading) AS l
      WHERE i.indrelid = c.conrelid
        AND l.leading @> c.conkey
        AND l.leading <@ c.conkey
  )
ORDER BY table_name, constraint_name;
