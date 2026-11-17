-- Stage 3 contract: book_seat() and buy_standing() keep every customer at 4 tickets per show
-- at most (active seats plus standing places), also when one customer buys twice at once.

-- Ana (customer 1) already holds 3 tickets for show 1. Session A buys one more and stays
-- open; session B, also for Ana, tries another one.
CREATE FUNCTION seat_test.limit_race(a_sql text, b_sql text) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    held integer;
BEGIN
    PERFORM seat_test.reset();
    PERFORM seat_test.open('a', 'b');
    PERFORM dblink_exec('a', 'DO $do$ BEGIN PERFORM book_seat(1, s, 1) FROM unnest(ARRAY[''B1'', ''B2'', ''B3'']) AS s; END $do$');
    PERFORM dblink_exec('a', 'BEGIN');
    a := seat_test.run('a', a_sql);
    PERFORM seat_test.expect(a IS NOT NULL AND a NOT LIKE 'error:%',
        format('with 3 tickets, Ana''s 4th (%s) should succeed, got %s', a_sql, coalesce(a, 'NULL')));
    PERFORM seat_test.start('b', b_sql);
    b_waited := seat_test.waiting('b');
    PERFORM dblink_exec('a', 'COMMIT');
    b := seat_test.finish('b');
    PERFORM seat_test.close_all();
    held := (SELECT count(*) FROM bookings WHERE show_id = 1 AND customer_id = 1 AND cancelled_at IS NULL)
          + (SELECT coalesce(sum(quantity), 0) FROM standing_tickets WHERE show_id = 1 AND customer_id = 1);
    PERFORM seat_test.expect(b_waited, format(
        'Ana''s second purchase (%s) should wait for her first one to finish, but it finished at once with %s',
        b_sql, coalesce(b, 'NULL')));
    PERFORM seat_test.expect(b IS NULL, format(
        'Ana''s 5th ticket (%s) should return NULL, got %s', b_sql, b));
    PERFORM seat_test.expect(held = 4, format('Ana should end with 4 tickets for show 1, she has %s', held));
    PERFORM seat_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM seat_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_ok(
    'four seats, then no more',
    NULL,
    $$SELECT (SELECT count(*) = 4 FROM r WHERE id IS NOT NULL) AND (SELECT id IS NULL FROM fifth)$$,
    'Ana may book 4 seats of show 1; the 5th book_seat should return NULL',
    setup => $$SELECT seat_test.require('booking.sql', 'standing.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT book_seat(1, s, 1) AS id FROM unnest(ARRAY['A1', 'A2', 'A3', 'A4']) AS s;
               CREATE TEMP TABLE fifth AS SELECT book_seat(1, 'A5', 1) AS id$$
);

SELECT dojo.script_ok(
    'standing places count towards the limit',
    NULL,
    $$SELECT (SELECT id IS NOT NULL FROM standing) AND (SELECT id IS NOT NULL FROM seat)
         AND (SELECT id IS NULL FROM extra_seat) AND (SELECT id IS NULL FROM extra_standing)$$,
    'with 3 standing places and 1 seat, Ana holds 4: one more seat or standing place should return NULL',
    setup => $$SELECT seat_test.require('booking.sql', 'standing.sql')$$,
    after => $$CREATE TEMP TABLE standing AS SELECT buy_standing(1, 1, 3) AS id;
               CREATE TEMP TABLE seat AS SELECT book_seat(1, 'A1', 1) AS id;
               CREATE TEMP TABLE extra_seat AS SELECT book_seat(1, 'A2', 1) AS id;
               CREATE TEMP TABLE extra_standing AS SELECT buy_standing(1, 1, 1) AS id$$
);

SELECT dojo.script_ok(
    'one sale over the limit sells nothing',
    NULL,
    $$SELECT (SELECT id IS NULL FROM too_many) AND standing_left(1) = 5$$,
    'buy_standing(1, 1, 5) is 5 tickets for one customer: it should return NULL and sell nothing',
    setup => $$SELECT seat_test.require('booking.sql', 'standing.sql')$$,
    after => $$CREATE TEMP TABLE too_many AS SELECT buy_standing(1, 1, 5) AS id$$
);

SELECT dojo.script_ok(
    'a cancelled booking no longer counts',
    NULL,
    $$SELECT id IS NOT NULL FROM again$$,
    'after cancelling one of her 4 seats, Ana may book another one',
    setup => $$SELECT seat_test.require('booking.sql', 'standing.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT book_seat(1, s, 1) AS id FROM unnest(ARRAY['A1', 'A2', 'A3', 'A4']) AS s;
               SELECT cancel_booking(min(id)) FROM r;
               CREATE TEMP TABLE again AS SELECT book_seat(1, 'A5', 1) AS id$$
);

SELECT dojo.script_ok(
    'the limit is per show',
    NULL,
    $$SELECT (SELECT id IS NOT NULL FROM other_show) AND (SELECT id IS NOT NULL FROM other_customer)$$,
    'with 4 tickets for show 1, Ana may still book show 2, and Bia may still book show 1',
    setup => $$SELECT seat_test.require('booking.sql', 'standing.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT book_seat(1, s, 1) AS id FROM unnest(ARRAY['A1', 'A2', 'A3', 'A4']) AS s;
               CREATE TEMP TABLE other_show AS SELECT book_seat(2, 'A1', 1) AS id;
               CREATE TEMP TABLE other_customer AS SELECT book_seat(1, 'A5', 2) AS id$$
);

SELECT dojo.script_ok(
    'one customer booking two seats at once stays within the limit',
    NULL,
    $$SELECT seat_test.limit_race('SELECT book_seat(1, ''A1'', 1)', 'SELECT book_seat(1, ''A2'', 1)')$$,
    'Ana''s two bookings at once went wrong',
    setup => $$SELECT seat_test.require('booking.sql', 'standing.sql')$$
);

SELECT dojo.script_ok(
    'a seat and a standing place at once stay within the limit',
    NULL,
    $$SELECT seat_test.limit_race('SELECT buy_standing(1, 1, 1)', 'SELECT book_seat(1, ''A2'', 1)')$$,
    'Ana''s seat and standing place at once went wrong',
    setup => $$SELECT seat_test.require('booking.sql', 'standing.sql')$$
);
