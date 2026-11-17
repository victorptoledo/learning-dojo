-- Standing tickets. "No more than the capacity" is a rule over many rows, which no CHECK or
-- UNIQUE can express, so the sale serializes on the one row every buyer of the show shares:
-- the show itself, locked with FOR UPDATE. A buyer of another show locks another row.

CREATE TABLE standing_tickets (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id     integer NOT NULL REFERENCES shows,
    customer_id integer NOT NULL REFERENCES customers,
    quantity    integer NOT NULL CHECK (quantity > 0),
    bought_at   timestamptz NOT NULL DEFAULT now()
);

CREATE FUNCTION standing_left(p_show integer) RETURNS integer
LANGUAGE sql STABLE AS $$
    SELECT s.standing_capacity
           - coalesce((SELECT sum(t.quantity) FROM standing_tickets t WHERE t.show_id = s.id), 0)
    FROM shows s
    WHERE s.id = p_show
$$;

CREATE FUNCTION buy_standing(p_show integer, p_customer integer, p_quantity integer) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    ticket_id bigint;
BEGIN
    -- Waits for any other sale of this show to finish; the count below then sees its tickets.
    PERFORM FROM shows WHERE id = p_show FOR UPDATE;
    IF standing_left(p_show) < p_quantity THEN
        RETURN NULL;
    END IF;
    INSERT INTO standing_tickets (show_id, customer_id, quantity)
    VALUES (p_show, p_customer, p_quantity)
    RETURNING id INTO ticket_id;
    RETURN ticket_id;
END
$$;
