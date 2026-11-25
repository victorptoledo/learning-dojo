-- The term is a parameter, never part of the SQL text, and strpos compares it literally,
-- so neither quotes nor LIKE wildcards in it mean anything.
CREATE OR REPLACE FUNCTION search_customers(term text) RETURNS TABLE (id integer, name text)
LANGUAGE sql STABLE AS $$
    SELECT c.id, c.name
    FROM customers c
    WHERE strpos(lower(c.name), lower(term)) > 0
    ORDER BY c.id
$$;
