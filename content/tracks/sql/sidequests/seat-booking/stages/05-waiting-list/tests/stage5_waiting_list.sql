-- Stage 5 contract: waitlist.sql creates waitlist, join_waitlist() and offer_next(); two box
-- offices share the list without waiting for each other or offering anyone twice.

-- Ana, Bia and Caio wait for show 1, in that order. Box office A offers the next one and
-- stays open; box office B offers the next one too.
CREATE FUNCTION seat_test.two_box_offices(first_ends text) RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    third text;
    still_waiting integer;
BEGIN
    PERFORM seat_test.reset();
    PERFORM seat_test.open('a', 'b');
    PERFORM dblink_exec('a', 'DO $do$ BEGIN PERFORM join_waitlist(1, c) FROM unnest(ARRAY[1, 2, 3]) AS c; END $do$');
    PERFORM dblink_exec('a', 'BEGIN');
    a := seat_test.run('a', 'SELECT offer_next(1)');
    PERFORM seat_test.expect(a = '1', format('the first offer_next(1) should offer Ana (1), got %s', coalesce(a, 'NULL')));
    PERFORM seat_test.start('b', 'SELECT offer_next(1)');
    b_waited := seat_test.waiting('b');
    b := seat_test.finish('b');
    PERFORM dblink_exec('a', first_ends);
    third := seat_test.run('b', 'SELECT offer_next(1)');
    PERFORM seat_test.close_all();
    SELECT count(*) INTO still_waiting FROM waitlist WHERE show_id = 1 AND offered_at IS NULL;
    PERFORM seat_test.expect(NOT b_waited,
        'the second box office waited for the first one: it should skip the entry the first one is offering');
    PERFORM seat_test.expect(b = '2', format(
        'while box office A is offering Ana, box office B should offer Bia (2), got %s', coalesce(b, 'NULL')));
    IF first_ends = 'COMMIT' THEN
        PERFORM seat_test.expect(third = '3', format(
            'after Ana and Bia, the next offer should be Caio (3), got %s', coalesce(third, 'NULL')));
        PERFORM seat_test.expect(still_waiting = 0, format('nobody should still wait, %s do', still_waiting));
    ELSE
        PERFORM seat_test.expect(third = '1', format(
            'box office A rolled back its offer to Ana, so the next offer should be Ana (1) again, got %s',
            coalesce(third, 'NULL')));
        PERFORM seat_test.expect(still_waiting = 1, format('Caio should still wait, %s entries do', still_waiting));
    END IF;
    PERFORM seat_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM seat_test.close_all();
    RAISE;
END
$$;

-- Two sessions put the same customer on the list at once.
CREATE FUNCTION seat_test.join_twice() RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    entries integer;
BEGIN
    PERFORM seat_test.reset();
    PERFORM seat_test.open('a', 'b');
    PERFORM dblink_exec('a', 'BEGIN');
    a := seat_test.run('a', 'SELECT join_waitlist(1, 4)');
    PERFORM seat_test.start('b', 'SELECT join_waitlist(1, 4)');
    b_waited := seat_test.waiting('b');
    PERFORM dblink_exec('a', 'COMMIT');
    b := seat_test.finish('b');
    PERFORM seat_test.close_all();
    SELECT count(*) INTO entries FROM waitlist WHERE show_id = 1 AND customer_id = 4;
    PERFORM seat_test.expect(b_waited,
        'the second join of Dani should wait for the first one, but it finished at once with ' || coalesce(b, 'NULL'));
    PERFORM seat_test.expect(b IS NOT NULL AND b NOT LIKE 'error:%' AND b = a, format(
        'both joins should return the same entry id, got %s and %s', a, coalesce(b, 'NULL')));
    PERFORM seat_test.expect(entries = 1, format('Dani should be on the list once, she is there %s times', entries));
    PERFORM seat_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM seat_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_ok(
    'offers in the order people joined',
    NULL,
    $$SELECT array_agg(customer ORDER BY n) = ARRAY[3, 1, 2, NULL] FROM offers$$,
    'Caio, Ana and Bia joined in that order: offer_next(1) should return 3, 1, 2, then NULL',
    setup => $$SELECT seat_test.require('waitlist.sql')$$,
    after => $$SELECT join_waitlist(1, 3); SELECT join_waitlist(1, 1); SELECT join_waitlist(1, 2);
               CREATE TEMP TABLE offers (n integer, customer integer);
               INSERT INTO offers SELECT 1, offer_next(1);
               INSERT INTO offers SELECT 2, offer_next(1);
               INSERT INTO offers SELECT 3, offer_next(1);
               INSERT INTO offers SELECT 4, offer_next(1)$$
);

SELECT dojo.script_ok(
    'each show has its own list',
    NULL,
    $$SELECT (SELECT customer = 2 FROM first_of_2) AND (SELECT customer = 1 FROM first_of_1)$$,
    'offer_next(2) should offer the first one waiting for show 2, not for show 1',
    setup => $$SELECT seat_test.require('waitlist.sql')$$,
    after => $$SELECT join_waitlist(1, 1); SELECT join_waitlist(2, 2);
               CREATE TEMP TABLE first_of_2 AS SELECT offer_next(2) AS customer;
               CREATE TEMP TABLE first_of_1 AS SELECT offer_next(1) AS customer$$
);

SELECT dojo.script_ok(
    'joining twice keeps one entry',
    NULL,
    $$SELECT (SELECT a = b FROM twice) AND (SELECT count(*) = 1 FROM waitlist WHERE customer_id = 1)$$,
    'join_waitlist(1, 1) twice should return the same id and leave one entry',
    setup => $$SELECT seat_test.require('waitlist.sql')$$,
    after => $$CREATE TEMP TABLE twice AS SELECT join_waitlist(1, 1) AS a;
               ALTER TABLE twice ADD COLUMN b bigint;
               UPDATE twice SET b = join_waitlist(1, 1)$$
);

SELECT dojo.script_ok(
    'after an offer, a customer may join again',
    NULL,
    $$SELECT (SELECT second_entry IS NOT NULL AND second_entry <> first_entry FROM entries)
         AND (SELECT count(*) = 2 FROM waitlist WHERE customer_id = 1)$$,
    'once Ana was offered a ticket, join_waitlist(1, 1) should create a new entry',
    setup => $$SELECT seat_test.require('waitlist.sql')$$,
    after => $$CREATE TEMP TABLE entries AS SELECT join_waitlist(1, 1) AS first_entry, NULL::bigint AS second_entry;
               SELECT offer_next(1);
               UPDATE entries SET second_entry = join_waitlist(1, 1)$$
);

SELECT dojo.script_ok(
    'an empty list offers nobody',
    NULL,
    $$SELECT offer_next(1) IS NULL$$,
    'offer_next on a show nobody waits for should return NULL',
    setup => $$SELECT seat_test.require('waitlist.sql')$$
);

SELECT dojo.script_fails(
    'the table itself keeps one waiting entry per customer',
    NULL,
    $$INSERT INTO waitlist (show_id, customer_id) VALUES (1, 1), (1, 1)$$,
    '23505',
    'a direct INSERT of two waiting entries of Ana for show 1 must fail',
    setup => $$SELECT seat_test.require('waitlist.sql')$$
);

SELECT dojo.script_ok(
    'two box offices offer two different people without waiting',
    NULL,
    $$SELECT seat_test.two_box_offices('COMMIT')$$,
    'the two box offices went wrong',
    setup => $$SELECT seat_test.require('waitlist.sql')$$
);

SELECT dojo.script_ok(
    'an offer rolled back goes back to the list',
    NULL,
    $$SELECT seat_test.two_box_offices('ROLLBACK')$$,
    'the two box offices went wrong',
    setup => $$SELECT seat_test.require('waitlist.sql')$$
);

SELECT dojo.script_ok(
    'two sessions joining the same customer keep one entry',
    NULL,
    $$SELECT seat_test.join_twice()$$,
    'joining Dani twice at once went wrong',
    setup => $$SELECT seat_test.require('waitlist.sql')$$
);
