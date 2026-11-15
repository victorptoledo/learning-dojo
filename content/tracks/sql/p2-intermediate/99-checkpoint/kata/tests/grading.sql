-- Valid rows that every schema must accept, inserted after schema.sql.
\set base 'INSERT INTO venues VALUES (1, ''Main Hall'', 3), (2, ''Garden'', 100); INSERT INTO events VALUES (10, 1, ''Kata Night'', ''2026-12-01 19:00+00'', ''2026-12-01 22:00+00''), (11, 1, ''Belt Exam'', ''2026-12-05 10:00+00'', ''2026-12-05 13:00+00''), (12, 2, ''Open Air Training'', ''2026-12-10 09:00+00'', ''2026-12-10 11:00+00''); INSERT INTO customers VALUES (100, ''ada@example.com'', ''Ada''), (101, ''grace@example.com'', ''Grace''), (102, ''linus@example.com'', ''Linus''); INSERT INTO tickets VALUES (1000, 10, 100, ''A1'', 15.00), (1001, 10, 101, ''A2'', 15.00), (1002, 11, 100, ''A1'', 0.00), (1003, 12, 102, ''G7'', 5.00)'

SELECT dojo.script_ok(
    'accepts valid venues, events, customers and tickets',
    'schema.sql',
    $$SELECT (SELECT count(*) FROM tickets) = 4$$,
    'the valid rows were not all stored',
    after => :'base'
);

SELECT dojo.script_fails(
    'rejects a venue without a name', 'schema.sql',
    $$INSERT INTO venues VALUES (3, NULL, 10)$$, '23502',
    'a venue needs a name', after => :'base'
);

SELECT dojo.script_fails(
    'rejects a venue with no capacity', 'schema.sql',
    $$INSERT INTO venues VALUES (3, 'Basement', 0)$$, '23514',
    'a capacity must be greater than 0', after => :'base'
);

SELECT dojo.script_fails(
    'rejects two venues with the same name', 'schema.sql',
    $$INSERT INTO venues VALUES (3, 'Garden', 50)$$, '23505',
    'venue names are unique', after => :'base'
);

SELECT dojo.script_fails(
    'rejects an event that ends before it starts', 'schema.sql',
    $$INSERT INTO events VALUES (13, 2, 'Time Travel', '2026-12-10 12:00+00', '2026-12-10 11:00+00')$$, '23514',
    'an event must end after it starts', after => :'base'
);

SELECT dojo.script_fails(
    'rejects an event that ends when it starts', 'schema.sql',
    $$INSERT INTO events VALUES (13, 2, 'Blink', '2026-12-10 12:00+00', '2026-12-10 12:00+00')$$, '23514',
    'an event must end after it starts', after => :'base'
);

SELECT dojo.script_fails(
    'rejects an event in a venue that does not exist', 'schema.sql',
    $$INSERT INTO events VALUES (13, 99, 'Nowhere', '2026-12-10 12:00+00', '2026-12-10 13:00+00')$$, '23503',
    'an event must take place in an existing venue', after => :'base'
);

SELECT dojo.script_fails(
    'rejects an event without a start', 'schema.sql',
    $$INSERT INTO events VALUES (13, 2, 'Someday', NULL, '2026-12-10 13:00+00')$$, '23502',
    'an event needs a start and an end', after => :'base'
);

SELECT dojo.script_fails(
    'rejects two customers with the same email', 'schema.sql',
    $$INSERT INTO customers VALUES (103, 'ada@example.com', 'Another Ada')$$, '23505',
    'one customer per email', after => :'base'
);

SELECT dojo.script_fails(
    'rejects the same seat sold twice for one event', 'schema.sql',
    $$INSERT INTO tickets VALUES (1004, 10, 102, 'A1', 15.00)$$, '23505',
    'a seat is sold at most once per event', after => :'base'
);

SELECT dojo.script_ok(
    'accepts the same seat at different events', 'schema.sql',
    $$SELECT count(*) = 2 FROM tickets WHERE seat = 'A1'$$,
    'seat A1 of two different events must be allowed', after => :'base'
);

SELECT dojo.script_fails(
    'rejects a negative price', 'schema.sql',
    $$INSERT INTO tickets VALUES (1004, 12, 101, 'G8', -1.00)$$, '23514',
    'a price cannot be negative (0 is a free ticket)', after => :'base'
);

SELECT dojo.script_fails(
    'rejects a ticket for an event that does not exist', 'schema.sql',
    $$INSERT INTO tickets VALUES (1004, 99, 101, 'Z1', 1.00)$$, '23503',
    'a ticket must be for an existing event', after => :'base'
);

SELECT dojo.script_fails(
    'keeps a customer who holds tickets', 'schema.sql',
    $$DELETE FROM customers WHERE id = 100$$, '23503',
    'a customer with tickets cannot be deleted', after => :'base'
);

SELECT dojo.script_eq(
    'deleting an event deletes its tickets', 'schema.sql',
    $$SELECT id FROM tickets$$,
    $$VALUES (1002), (1003)$$,
    after => :'base' || '; DELETE FROM events WHERE id = 10'
);

SELECT dojo.query_eq(
    'reports the availability of every event',
    'availability.sql',
    $$VALUES ('Open Air Training', 'Garden', 1, 99, 1),
             ('Kata Night', 'Main Hall', 2, 1, 1),
             ('Belt Exam', 'Main Hall', 1, 2, 2)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); $$ || :'base'
);

SELECT dojo.query_eq(
    'an event without tickets has every seat left and ties share a rank',
    'availability.sql',
    $$VALUES ('Open Air Training', 'Garden', 1, 99, 1),
             ('Kata Night', 'Main Hall', 2, 1, 1),
             ('Belt Exam', 'Main Hall', 1, 2, 2),
             ('Grading Day', 'Main Hall', 1, 2, 2),
             ('Closing Party', 'Main Hall', 0, 3, 3)$$,
    ordered => true,
    setup => $$SELECT dojo.run_file('schema.sql'); $$ || :'base' || $$;
        INSERT INTO events VALUES (13, 1, 'Grading Day', '2026-12-12 10:00+00', '2026-12-12 12:00+00'),
                                  (14, 1, 'Closing Party', '2026-12-20 20:00+00', '2026-12-20 23:00+00');
        INSERT INTO tickets VALUES (1005, 13, 102, 'B1', 10.00)$$
);
