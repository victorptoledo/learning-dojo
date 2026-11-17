-- Stage 4 contract: rooms.sql creates room_bookings and book_room(); bookings of a room
-- never overlap, also when two sessions book at once.

-- Session A books a slot and stays open; session B books another slot.
CREATE FUNCTION seat_test.room_race(b_room integer, b_from text, b_to text, first_ends text,
                                    b_should_wait boolean, b_should_get boolean)
RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    b_call text := format('SELECT book_room(%s, 2, %L, %L)', b_room, b_from, b_to);
    overlapping integer;
BEGIN
    PERFORM seat_test.reset();
    PERFORM seat_test.open('a', 'b');
    PERFORM dblink_exec('a', 'BEGIN');
    a := seat_test.run('a', 'SELECT book_room(1, 1, ''2026-12-01 10:00+00'', ''2026-12-01 12:00+00'')');
    PERFORM seat_test.expect(a IS NOT NULL AND a NOT LIKE 'error:%',
        format('booking Studio A from 10:00 to 12:00 should return an id, got %s', coalesce(a, 'NULL')));
    PERFORM seat_test.start('b', b_call);
    b_waited := seat_test.waiting('b');
    PERFORM dblink_exec('a', first_ends);
    b := seat_test.finish('b');
    PERFORM seat_test.close_all();
    SELECT count(*) INTO overlapping
    FROM room_bookings x JOIN room_bookings y ON x.id < y.id AND x.room_id = y.room_id AND x.during && y.during;
    IF b_should_wait THEN
        PERFORM seat_test.expect(b_waited, format(
            '%s overlaps the open booking from 10:00 to 12:00, so it should wait for it, but it finished at once with %s',
            b_call, coalesce(b, 'NULL')));
    ELSE
        PERFORM seat_test.expect(NOT b_waited, format(
            '%s does not overlap the open booking from 10:00 to 12:00 of Studio A, yet it waited for it', b_call));
    END IF;
    PERFORM seat_test.expect(b IS NULL OR b NOT LIKE 'error:%', format('%s failed: %s', b_call, b));
    IF b_should_get THEN
        PERFORM seat_test.expect(b IS NOT NULL, format('%s should get the room, got NULL', b_call));
    ELSE
        PERFORM seat_test.expect(b IS NULL, format('%s should return NULL (the room is taken), got %s', b_call, b));
    END IF;
    PERFORM seat_test.expect(overlapping = 0, 'two bookings of a room overlap');
    PERFORM seat_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM seat_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_ok(
    'books a free room',
    NULL,
    $$SELECT (SELECT id IS NOT NULL FROM r)
         AND (SELECT count(*) = 1 FROM room_bookings b JOIN r USING (id)
              WHERE b.room_id = 1 AND b.customer_id = 1
                AND b.during = tstzrange('2026-12-01 10:00+00', '2026-12-01 12:00+00', '[)'))$$,
    'book_room(1, 1, 10:00, 12:00) should return the id of a booking whose during is [10:00, 12:00)',
    setup => $$SELECT seat_test.require('rooms.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT book_room(1, 1, '2026-12-01 10:00+00', '2026-12-01 12:00+00') AS id$$
);

SELECT dojo.script_ok(
    'an overlapping booking returns NULL',
    NULL,
    $$SELECT (SELECT id IS NULL FROM later) AND (SELECT id IS NULL FROM inside) AND (SELECT id IS NULL FROM around)
         AND (SELECT count(*) = 1 FROM room_bookings)$$,
    'with Studio A booked from 10:00 to 12:00, 11:00-13:00, 10:30-11:00 and 09:00-14:00 should all return NULL',
    setup => $$SELECT seat_test.require('rooms.sql')$$,
    after => $$SELECT book_room(1, 1, '2026-12-01 10:00+00', '2026-12-01 12:00+00');
               CREATE TEMP TABLE later AS SELECT book_room(1, 2, '2026-12-01 11:00+00', '2026-12-01 13:00+00') AS id;
               CREATE TEMP TABLE inside AS SELECT book_room(1, 2, '2026-12-01 10:30+00', '2026-12-01 11:00+00') AS id;
               CREATE TEMP TABLE around AS SELECT book_room(1, 2, '2026-12-01 09:00+00', '2026-12-01 14:00+00') AS id$$
);

SELECT dojo.script_ok(
    'back to back is not an overlap',
    NULL,
    $$SELECT (SELECT id IS NOT NULL FROM before) AND (SELECT id IS NOT NULL FROM after_it)$$,
    'with Studio A booked from 10:00 to 12:00, 08:00-10:00 and 12:00-14:00 should both be booked',
    setup => $$SELECT seat_test.require('rooms.sql')$$,
    after => $$SELECT book_room(1, 1, '2026-12-01 10:00+00', '2026-12-01 12:00+00');
               CREATE TEMP TABLE before AS SELECT book_room(1, 2, '2026-12-01 08:00+00', '2026-12-01 10:00+00') AS id;
               CREATE TEMP TABLE after_it AS SELECT book_room(1, 2, '2026-12-01 12:00+00', '2026-12-01 14:00+00') AS id$$
);

SELECT dojo.script_ok(
    'another room is free at the same time',
    NULL,
    $$SELECT id IS NOT NULL FROM other$$,
    'Studio B is free while Studio A is booked',
    setup => $$SELECT seat_test.require('rooms.sql')$$,
    after => $$SELECT book_room(1, 1, '2026-12-01 10:00+00', '2026-12-01 12:00+00');
               CREATE TEMP TABLE other AS SELECT book_room(2, 2, '2026-12-01 10:00+00', '2026-12-01 12:00+00') AS id$$
);

SELECT dojo.script_fails(
    'an empty slot is refused',
    NULL,
    $$SELECT book_room(1, 1, '2026-12-01 10:00+00', '2026-12-01 10:00+00')$$,
    '23514',
    'a booking from 10:00 to 10:00 is empty: room_bookings must refuse it with a CHECK',
    setup => $$SELECT seat_test.require('rooms.sql')$$
);

SELECT dojo.script_fails(
    'a slot that ends before it starts is refused',
    NULL,
    $$SELECT book_room(1, 1, '2026-12-01 12:00+00', '2026-12-01 10:00+00')$$,
    '22',
    'a booking from 12:00 to 10:00 is not a time range',
    setup => $$SELECT seat_test.require('rooms.sql')$$
);

SELECT dojo.script_fails(
    'the table itself refuses overlapping bookings',
    NULL,
    $$INSERT INTO room_bookings (room_id, customer_id, during) VALUES
          (1, 1, tstzrange('2026-12-01 10:00+00', '2026-12-01 12:00+00')),
          (1, 2, tstzrange('2026-12-01 11:00+00', '2026-12-01 13:00+00'))$$,
    '23P01',
    'a direct INSERT of two overlapping bookings of a room must fail with an exclusion violation',
    setup => $$SELECT seat_test.require('rooms.sql')$$
);

SELECT dojo.script_ok(
    'two sessions racing for overlapping slots: one gets the room',
    NULL,
    $$SELECT seat_test.room_race(1, '2026-12-01 11:00+00', '2026-12-01 13:00+00', 'COMMIT', true, false)$$,
    'the race for Studio A went wrong',
    setup => $$SELECT seat_test.require('rooms.sql')$$
);

SELECT dojo.script_ok(
    'if the first booking rolls back, the overlapping one gets the room',
    NULL,
    $$SELECT seat_test.room_race(1, '2026-12-01 11:00+00', '2026-12-01 13:00+00', 'ROLLBACK', true, true)$$,
    'the race for Studio A went wrong',
    setup => $$SELECT seat_test.require('rooms.sql')$$
);

SELECT dojo.script_ok(
    'slots that do not overlap do not wait',
    NULL,
    $$SELECT seat_test.room_race(1, '2026-12-01 12:00+00', '2026-12-01 14:00+00', 'COMMIT', false, true)
         AND seat_test.room_race(2, '2026-12-01 10:00+00', '2026-12-01 12:00+00', 'COMMIT', false, true)$$,
    'booking a free slot while another one is open went wrong',
    setup => $$SELECT seat_test.require('rooms.sql')$$
);
