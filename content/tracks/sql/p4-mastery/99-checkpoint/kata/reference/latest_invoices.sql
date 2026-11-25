-- One query for the whole page: the ids arrive as one array parameter, and LATERAL runs the
-- "latest three" subquery per customer inside the same statement instead of once per round trip.
CREATE FUNCTION latest_invoices(ids integer[])
RETURNS TABLE (customer_id integer, period date, amount numeric)
LANGUAGE sql STABLE AS $$
    SELECT c.id, i.period, i.amount
    FROM customers c
    LEFT JOIN LATERAL (
        SELECT period, amount
        FROM invoices
        WHERE invoices.customer_id = c.id
        ORDER BY period DESC
        LIMIT 3
    ) i ON true
    WHERE c.id = ANY (ids)
    ORDER BY c.id, i.period DESC
$$;
