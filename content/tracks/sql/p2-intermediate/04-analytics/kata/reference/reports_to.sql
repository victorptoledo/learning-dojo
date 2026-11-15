WITH RECURSIVE chain AS (
    SELECT id, name, 0 AS depth, name::text AS path
    FROM employees
    WHERE name = 'Bruno Reis'

    UNION ALL

    SELECT e.id, e.name, chain.depth + 1, chain.path || ' > ' || e.name
    FROM employees AS e
    JOIN chain ON e.manager_id = chain.id
)
SELECT name, depth, path
FROM chain
ORDER BY path;
