-- Reads tables, so STABLE: one result per statement, never cached across changes.
CREATE FUNCTION open_tickets_for(p_plan text)
RETURNS TABLE (id bigint, title text, priority integer)
LANGUAGE plpgsql STABLE
AS $$
BEGIN
    IF p_plan IS NULL OR p_plan NOT IN ('free', 'pro', 'enterprise') THEN
        RAISE EXCEPTION 'unknown plan: %', p_plan USING ERRCODE = '22023';
    END IF;
    RETURN QUERY
        SELECT t.id, t.title, t.priority
        FROM tickets AS t
        WHERE t.status = 'open'
          AND t.metadata @> jsonb_build_object('customer', jsonb_build_object('plan', p_plan))
          AND t.metadata @> '{"tags": ["urgent"]}'
        ORDER BY t.priority DESC, t.id;
END
$$;
