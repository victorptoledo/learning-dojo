-- Rehearsal rooms by the hour. "No two bookings of a room overlap" compares each row with
-- every other: an exclusion constraint is the UNIQUE of ranges, and like UNIQUE it makes a
-- second, overlapping insert wait for the first transaction. btree_gist lets the GiST index
-- compare room_id with =.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE room_bookings (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id     integer NOT NULL REFERENCES rooms,
    customer_id integer NOT NULL REFERENCES customers,
    during      tstzrange NOT NULL CHECK (NOT isempty(during)),
    EXCLUDE USING gist (room_id WITH =, during WITH &&)
);

-- [from, to): a booking until 12:00 and one from 12:00 do not overlap.
CREATE FUNCTION book_room(p_room integer, p_customer integer, p_from timestamptz, p_to timestamptz)
RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    booking_id bigint;
BEGIN
    INSERT INTO room_bookings (room_id, customer_id, during)
    VALUES (p_room, p_customer, tstzrange(p_from, p_to, '[)'))
    ON CONFLICT DO NOTHING
    RETURNING id INTO booking_id;
    RETURN booking_id;
END
$$;
