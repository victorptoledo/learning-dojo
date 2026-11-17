-- Stage 2 contract: standing.sql creates standing_tickets, standing_left() and buy_standing().

-- Two sessions buy standing tickets; the first one is still open when the second starts.
CREATE FUNCTION seat_test.standing_race(show_a integer, qty_a integer, show_b integer, qty_b integer,
                                        first_ends text, b_should_wait boolean, b_should_get boolean)
RETURNS boolean
LANGUAGE plpgsql AS $$
DECLARE
    a text;
    b text;
    b_waited boolean;
    oversold integer;
BEGIN
    PERFORM seat_test.reset();
    PERFORM seat_test.open('a', 'b');
    PERFORM dblink_exec('a', 'BEGIN');
    a := seat_test.run('a', format('SELECT buy_standing(%s, 1, %s)', show_a, qty_a));
    PERFORM seat_test.expect(a IS NOT NULL AND a NOT LIKE 'error:%',
        format('the first buy_standing(%s, 1, %s) should return a ticket id, got %s', show_a, qty_a, coalesce(a, 'NULL')));
    PERFORM seat_test.start('b', format('SELECT buy_standing(%s, 2, %s)', show_b, qty_b));
    b_waited := seat_test.waiting('b');
    PERFORM dblink_exec('a', first_ends);
    b := seat_test.finish('b');
    PERFORM seat_test.close_all();
    SELECT count(*) INTO oversold FROM shows WHERE standing_left(id) < 0;
    IF b_should_wait THEN
        PERFORM seat_test.expect(b_waited, format(
            'buy_standing(%s, 2, %s) should wait for the open sale of the same show, but it finished at once with %s',
            show_b, qty_b, coalesce(b, 'NULL')));
    ELSE
        PERFORM seat_test.expect(NOT b_waited, format(
            'buy_standing(%s, 2, %s) waited for a sale of show %s: lock only what the sale needs', show_b, qty_b, show_a));
    END IF;
    PERFORM seat_test.expect(b IS NULL OR b NOT LIKE 'error:%', 'the second sale failed: ' || b);
    IF b_should_get THEN
        PERFORM seat_test.expect(b IS NOT NULL, format('buy_standing(%s, 2, %s) should get its tickets, got NULL', show_b, qty_b));
    ELSE
        PERFORM seat_test.expect(b IS NULL, format(
            'buy_standing(%s, 2, %s) should return NULL (not enough room left), got %s', show_b, qty_b, b));
    END IF;
    PERFORM seat_test.expect(oversold = 0, 'a show sold more standing tickets than its capacity');
    PERFORM seat_test.reset();
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    PERFORM seat_test.close_all();
    RAISE;
END
$$;

SELECT dojo.script_ok(
    'sells standing tickets',
    NULL,
    $$SELECT (SELECT id IS NOT NULL FROM r)
         AND (SELECT count(*) = 1 FROM standing_tickets t JOIN r USING (id)
              WHERE t.show_id = 1 AND t.customer_id = 3 AND t.quantity = 2)
         AND standing_left(1) = 3$$,
    'buy_standing(1, 3, 2) should return the id of a ticket for 2, leaving 3 of 5 places',
    setup => $$SELECT seat_test.require('standing.sql')$$,
    after => $$CREATE TEMP TABLE r AS SELECT buy_standing(1, 3, 2) AS id$$
);

SELECT dojo.script_ok(
    'a sale bigger than what is left returns NULL',
    NULL,
    $$SELECT (SELECT id IS NOT NULL FROM first) AND (SELECT id IS NULL FROM too_many)
         AND (SELECT id IS NOT NULL FROM last_one) AND standing_left(2) = 0
         AND (SELECT count(*) = 2 FROM standing_tickets)$$,
    'show 2 holds 3: after 2 sold, 2 more must return NULL (and sell nothing) while 1 more still sells',
    setup => $$SELECT seat_test.require('standing.sql')$$,
    after => $$CREATE TEMP TABLE first AS SELECT buy_standing(2, 1, 2) AS id;
               CREATE TEMP TABLE too_many AS SELECT buy_standing(2, 2, 2) AS id;
               CREATE TEMP TABLE last_one AS SELECT buy_standing(2, 3, 1) AS id$$
);

SELECT dojo.script_ok(
    'a show without standing room sells none',
    NULL,
    $$SELECT buy_standing(3, 1, 1) IS NULL$$,
    'show 3 has no standing places',
    setup => $$SELECT seat_test.require('standing.sql')$$
);

SELECT dojo.script_fails(
    'a sale needs a positive quantity',
    NULL,
    $$SELECT buy_standing(1, 1, 0)$$,
    '23514',
    'standing_tickets.quantity must be more than 0',
    setup => $$SELECT seat_test.require('standing.sql')$$
);

SELECT dojo.script_ok(
    'what is left starts at the capacity',
    NULL,
    $$SELECT standing_left(1) = 5 AND standing_left(2) = 3 AND standing_left(3) = 0$$,
    'standing_left should return each show''s capacity when nothing is sold',
    setup => $$SELECT seat_test.require('standing.sql')$$
);

SELECT dojo.script_ok(
    'two sessions racing for the last places: no overselling',
    NULL,
    $$SELECT seat_test.standing_race(2, 2, 2, 2, 'COMMIT', true, false)$$,
    'the race for the last standing places went wrong',
    setup => $$SELECT seat_test.require('standing.sql')$$
);

SELECT dojo.script_ok(
    'if the first sale rolls back, the second one sells',
    NULL,
    $$SELECT seat_test.standing_race(2, 2, 2, 2, 'ROLLBACK', true, true)$$,
    'the race for the last standing places went wrong',
    setup => $$SELECT seat_test.require('standing.sql')$$
);

SELECT dojo.script_ok(
    'two sales that fit both sell',
    NULL,
    $$SELECT seat_test.standing_race(2, 1, 2, 2, 'COMMIT', true, true)$$,
    'two sales of 1 and 2 places fit in a show for 3',
    setup => $$SELECT seat_test.require('standing.sql')$$
);

SELECT dojo.script_ok(
    'sales of different shows do not wait for each other',
    NULL,
    $$SELECT seat_test.standing_race(1, 2, 2, 2, 'COMMIT', false, true)$$,
    'selling two shows at once went wrong',
    setup => $$SELECT seat_test.require('standing.sql')$$
);
