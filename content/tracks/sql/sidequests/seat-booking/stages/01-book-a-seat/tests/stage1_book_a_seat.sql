-- Stage 1 contract: booking.sql creates bookings, book_seat() and cancel_booking().

-- Two sessions book the same seat; the first one is still open when the second starts.
CREATE FUNCTION seat_test.same_seat(first_ends text) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    active bigint;
BEGIN
    PERFORM seat_test.reset();
    PERFORM seat_test.open('a', 'b');
    PERFORM dblink_exec('a', 'BEGIN');
    a := seat_test.run('a', 'SELECT book_seat(1, ''A1'', 1)');
    PERFORM seat_test.expect(a IS NOT NULL AND a NOT LIKE 'error:%',
        format('the first book_seat(1, ''A1'', 1) should return a booking id, got %s', coalesce(a, 'NULL')));
    PERFORM seat_test.start('b', 'SELECT book_seat(1, ''A1'', 2)');
    b_waited := seat_test.waiting('b');
    PERFORM dblink_exec('a', first_ends);
    b := seat_test.finish('b');
    PERFORM seat_test.close_all();
    SELECT count(*) INTO active FROM bookings WHERE show_id = 1 AND seat = 'A1' AND cancelled_at IS NULL;
    PERFORM seat_test.expect(b_waited,
        'the second session should wait for the first one''s open transaction, but it finished at once with ' || coalesce(b, 'NULL'));
    IF first_ends = 'COMMIT' THEN
        PERFORM seat_test.expect(b IS NULL,
            format('once the first booking committed, the second book_seat should return NULL (seat taken), got %s', b));
    ELSE
        PERFORM seat_test.expect(b IS NOT NULL AND b NOT LIKE 'error:%',
            format('once the first booking rolled back, the second book_seat should get the seat, got %s', coalesce(b, 'NULL')));
    END IF;
    PERFORM seat_test.expect(active = 1, format('A1 should have exactly 1 active booking, it has %s', active));
    PERFORM seat_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM seat_test.close_all();
    RAISE;
END
$$;

-- Two sessions book two different seats at the same time: neither waits.
CREATE FUNCTION seat_test.different_seats() RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    b text;
    b_waited boolean;
BEGIN
    PERFORM seat_test.reset();
    PERFORM seat_test.open('a', 'b');
    PERFORM dblink_exec('a', 'BEGIN');
    PERFORM seat_test.run('a', 'SELECT book_seat(1, ''A1'', 1)');
    PERFORM seat_test.start('b', 'SELECT book_seat(1, ''A2'', 2)');
    b_waited := seat_test.waiting('b');
    PERFORM dblink_exec('a', 'COMMIT');
    b := seat_test.finish('b');
    PERFORM seat_test.close_all();
    PERFORM seat_test.expect(NOT b_waited,
        'booking A2 waited for a transaction that booked A1: lock only what the booking needs');
    PERFORM seat_test.expect(b IS NOT NULL AND b NOT LIKE 'error:%',
        format('book_seat(1, ''A2'', 2) should return a booking id, got %s', coalesce(b, 'NULL')));
    PERFORM seat_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM seat_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_ok(
    'books a free seat',
    NULL,
    $$SELECT (SELECT id IS NOT NULL FROM r)
         AND (SELECT count(*) = 1 FROM bookings b JOIN r USING (id)
              WHERE b.show_id = 1 AND b.seat = 'A1' AND b.customer_id = 1 AND b.cancelled_at IS NULL)$$,
    'book_seat(1, ''A1'', 1) should return the id of a new active booking of A1 for customer 1',
    setup => $$SELECT seat_test.require('booking.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT book_seat(1, 'A1', 1) AS id$$
);

SELECT dojo.script_ok(
    'a taken seat returns NULL',
    NULL,
    $$SELECT (SELECT id IS NULL FROM second)
         AND (SELECT customer_id = 1 FROM bookings WHERE show_id = 1 AND seat = 'A1' AND cancelled_at IS NULL)$$,
    'booking A1 again should return NULL and leave the first booking alone',
    setup => $$SELECT seat_test.require('booking.sql')$$,
    after => $$CREATE TEMP TABLE first AS SELECT book_seat(1, 'A1', 1) AS id;
               CREATE TEMP TABLE second AS SELECT book_seat(1, 'A1', 2) AS id$$
);

SELECT dojo.script_ok(
    'the same seat of another show is free',
    NULL,
    $$SELECT id IS NOT NULL FROM second$$,
    'A1 of show 2 is another seat than A1 of show 1',
    setup => $$SELECT seat_test.require('booking.sql')$$,
    after => $$CREATE TEMP TABLE first AS SELECT book_seat(1, 'A1', 1) AS id;
               CREATE TEMP TABLE second AS SELECT book_seat(2, 'A1', 2) AS id$$
);

SELECT dojo.script_fails(
    'a seat that does not exist is refused',
    NULL,
    $$SELECT book_seat(1, 'Z9', 1)$$,
    '23503',
    'show 1 has no seat Z9: bookings must reference seats',
    setup => $$SELECT seat_test.require('booking.sql')$$
);

SELECT dojo.script_fails(
    'a customer that does not exist is refused',
    NULL,
    $$SELECT book_seat(1, 'A1', 999)$$,
    '23503',
    'there is no customer 999: bookings must reference customers',
    setup => $$SELECT seat_test.require('booking.sql')$$
);

SELECT dojo.script_ok(
    'cancelling frees the seat',
    NULL,
    $$SELECT (SELECT done FROM cancelled)
         AND (SELECT id IS NOT NULL FROM again)
         AND (SELECT count(*) = 2 AND count(cancelled_at) = 1 FROM bookings WHERE show_id = 1 AND seat = 'A1')$$,
    'cancel_booking should return true and keep the cancelled booking (with cancelled_at set); then A1 can be booked again',
    setup => $$SELECT seat_test.require('booking.sql')$$,
    after => $$CREATE TEMP TABLE first AS SELECT book_seat(1, 'A1', 1) AS id;
               CREATE TEMP TABLE cancelled AS SELECT cancel_booking(id) AS done FROM first;
               CREATE TEMP TABLE again AS SELECT book_seat(1, 'A1', 2) AS id$$
);

SELECT dojo.script_ok(
    'cancelling twice or an unknown booking returns false',
    NULL,
    $$SELECT (SELECT done = false FROM twice) AND (SELECT done = false FROM unknown)$$,
    'cancel_booking should return false for a booking already cancelled and for an id that does not exist',
    setup => $$SELECT seat_test.require('booking.sql')$$,
    after => $$CREATE TEMP TABLE first AS SELECT book_seat(1, 'A1', 1) AS id;
               SELECT cancel_booking(id) FROM first;
               CREATE TEMP TABLE twice AS SELECT cancel_booking(id) AS done FROM first;
               CREATE TEMP TABLE unknown AS SELECT cancel_booking(424242) AS done$$
);

SELECT dojo.script_fails(
    'the table itself refuses a double booking',
    NULL,
    $$INSERT INTO bookings (show_id, seat, customer_id) VALUES (1, 'B2', 1), (1, 'B2', 2)$$,
    '23505',
    'a direct INSERT of two active bookings of one seat must fail: the rule belongs in the table, not only in book_seat',
    setup => $$SELECT seat_test.require('booking.sql')$$
);

SELECT dojo.script_ok(
    'two sessions racing for a seat: one gets it',
    NULL,
    $$SELECT seat_test.same_seat('COMMIT')$$,
    'the race for A1 went wrong',
    setup => $$SELECT seat_test.require('booking.sql')$$
);

SELECT dojo.script_ok(
    'if the first session rolls back, the second gets the seat',
    NULL,
    $$SELECT seat_test.same_seat('ROLLBACK')$$,
    'the race for A1 went wrong',
    setup => $$SELECT seat_test.require('booking.sql')$$
);

SELECT dojo.script_ok(
    'bookings of different seats do not wait for each other',
    NULL,
    $$SELECT seat_test.different_seats()$$,
    'booking two seats at once went wrong',
    setup => $$SELECT seat_test.require('booking.sql')$$
);
