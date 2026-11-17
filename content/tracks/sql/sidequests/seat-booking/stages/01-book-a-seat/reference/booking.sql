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

-- Two sessions booking the same seat: the second INSERT waits on the first one's index
-- entry; when the first commits, ON CONFLICT DO NOTHING inserts nothing and RETURNING gives
-- no row, so the function returns NULL instead of failing.
CREATE FUNCTION book_seat(p_show integer, p_seat text, p_customer integer) RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    booking_id bigint;
BEGIN
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
