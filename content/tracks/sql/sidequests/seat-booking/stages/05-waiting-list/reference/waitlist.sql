-- The waiting list of sold-out shows. A customer waits at most once per show (a partial
-- unique index again), and two box offices take people off the list at the same time
-- without ever offering the same person twice or waiting for each other.

CREATE TABLE waitlist (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id     integer NOT NULL REFERENCES shows,
    customer_id integer NOT NULL REFERENCES customers,
    joined_at   timestamptz NOT NULL DEFAULT clock_timestamp(),
    offered_at  timestamptz
);

CREATE UNIQUE INDEX waitlist_once_per_show ON waitlist (show_id, customer_id) WHERE offered_at IS NULL;

-- Joining twice returns the entry already there. When another session is joining the same
-- customer at that moment, the INSERT waits for it; the SELECT after it, a new statement,
-- then sees the other session's committed row.
CREATE FUNCTION join_waitlist(p_show integer, p_customer integer) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    entry_id bigint;
BEGIN
    INSERT INTO waitlist (show_id, customer_id)
    VALUES (p_show, p_customer)
    ON CONFLICT (show_id, customer_id) WHERE offered_at IS NULL DO NOTHING
    RETURNING id INTO entry_id;
    IF entry_id IS NULL THEN
        SELECT id INTO entry_id
        FROM waitlist
        WHERE show_id = p_show AND customer_id = p_customer AND offered_at IS NULL;
    END IF;
    RETURN entry_id;
END
$$;

-- The oldest entry nobody else is offering right now: SKIP LOCKED passes over the rows
-- another box office has locked instead of waiting for them.
CREATE FUNCTION offer_next(p_show integer) RETURNS integer
LANGUAGE plpgsql AS $$
DECLARE
    offered integer;
BEGIN
    UPDATE waitlist
    SET offered_at = now()
    WHERE id = (
        SELECT id
        FROM waitlist
        WHERE show_id = p_show AND offered_at IS NULL
        ORDER BY joined_at, id
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    )
    RETURNING customer_id INTO offered;
    RETURN offered;
END
$$;
