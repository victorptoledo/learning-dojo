-- Seat bookings. A cancelled booking stays as history, so "one booking per seat" holds only
-- among the active ones: a partial unique index says exactly that, and the database enforces
-- it whatever the application does.

CREATE TABLE bookings (
    id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    show_id      integer NOT NULL,
    seat         text NOT NULL,
    customer_id  integer NOT NULL REFERENCES customers,
    booked_at    timestamptz NOT NULL DEFAULT now(),
    cancelled_at timestamptz,
    FOREIGN KEY (show_id, seat) REFERENCES seats
);

CREATE UNIQUE INDEX bookings_one_per_seat ON bookings (show_id, seat) WHERE cancelled_at IS NULL;

-- Tickets a customer holds for a show: active seats plus standing places. standing_tickets
-- comes in standing.sql; PL/pgSQL resolves the name when the function runs.
CREATE FUNCTION tickets_held(p_show integer, p_customer integer) RETURNS integer
LANGUAGE plpgsql STABLE AS $$
BEGIN
    RETURN (SELECT count(*) FROM bookings
            WHERE show_id = p_show AND customer_id = p_customer AND cancelled_at IS NULL)
         + (SELECT coalesce(sum(quantity), 0) FROM standing_tickets
            WHERE show_id = p_show AND customer_id = p_customer);
END
$$;

-- The limit is a rule over one customer's rows, so a customer's purchases take turns on the
-- customer's row. Other customers lock other rows and never wait.
CREATE FUNCTION book_seat(p_show integer, p_seat text, p_customer integer) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    booking_id bigint;
BEGIN
    PERFORM FROM customers WHERE id = p_customer FOR UPDATE;
    IF tickets_held(p_show, p_customer) + 1 > 4 THEN
        RETURN NULL;
    END IF;
    -- Two sessions booking the same seat: the second INSERT waits on the first one's index
    -- entry; when the first commits, ON CONFLICT DO NOTHING inserts nothing.
    INSERT INTO bookings (show_id, seat, customer_id)
    VALUES (p_show, p_seat, p_customer)
    ON CONFLICT (show_id, seat) WHERE cancelled_at IS NULL DO NOTHING
    RETURNING id INTO booking_id;
    RETURN booking_id;
END
$$;

CREATE FUNCTION cancel_booking(p_booking bigint) RETURNS boolean
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE bookings SET cancelled_at = now()
    WHERE id = p_booking AND cancelled_at IS NULL;
    RETURN FOUND;
END
$$;
