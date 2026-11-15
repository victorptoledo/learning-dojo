SELECT dojo.query_eq(
    'a book whose only loans are returned is on the shelf',
    'available_books.sql',
    $$VALUES ('Foundation'), ('I, Robot'), ('Kindred'), ('Stories of Your Life and Others'),
             ('The Dispossessed')$$,
    ordered => true,
    setup => $$UPDATE loans SET returned_on = DATE '2026-11-14' WHERE id = 2$$
);

SELECT dojo.query_eq(
    'reports the activity of every member',
    'member_activity.sql',
    $$VALUES ('Mia Chen', 3, 1), ('Noah Silva', 3, 1), ('Olivia Costa', 3, 2), ('Pedro Alves', 1, 0)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a member without loans shows zeros',
    'member_activity.sql',
    $$VALUES ('Mia Chen', 3, 1), ('Noah Silva', 3, 1), ('Olivia Costa', 3, 2), ('Pedro Alves', 1, 0),
             ('Quinn Rocha', 0, 0)$$,
    ordered => true,
    setup => $$INSERT INTO members VALUES (5, 'Quinn Rocha', DATE '2026-11-01')$$
);

SELECT dojo.query_eq(
    'lists the overdue loans',
    'overdue.sql',
    $$VALUES ('Olivia Costa', 'Parable of the Sower', 22), ('Olivia Costa', 'A Wizard of Earthsea', 20),
             ('Mia Chen', 'Foundation', 12)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a loan due today is not overdue and ties are ordered by member and title',
    'overdue.sql',
    $$VALUES ('Mia Chen', 'Foundation', 12), ('Noah Silva', 'Kindred', 12)$$,
    ordered => true,
    setup => $$DELETE FROM loans WHERE member_id = 3;
                INSERT INTO loans VALUES (11, 6, 2, DATE '2026-10-20', DATE '2026-11-03', NULL),
                                         (12, 8, 4, DATE '2026-11-10', DATE '2026-11-15', NULL)$$
);

SELECT dojo.query_eq(
    'counts the books and loans of prolific authors',
    'prolific_authors.sql',
    $$VALUES ('Ursula K. Le Guin', 3, 4), ('Isaac Asimov', 2, 3), ('Octavia E. Butler', 2, 3)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a prolific author whose books were never borrowed has zero loans',
    'prolific_authors.sql',
    $$VALUES ('Ursula K. Le Guin', 3, 4), ('Isaac Asimov', 2, 3), ('Octavia E. Butler', 2, 3),
             ('Ted Chiang', 2, 0)$$,
    ordered => true,
    setup => $$INSERT INTO books VALUES (9, 'Exhalation', 4, 2019);
                DELETE FROM loans WHERE book_id = 8$$
);

SELECT dojo.script_eq(
    'charges a fee for each late loan returned',
    'return_books.sql',
    $$SELECT loan_id, member_id, amount FROM fees$$,
    $$VALUES (5, 3, 5.50), (6, 3, 5.00)$$
);

SELECT dojo.script_eq(
    'closes every open loan of the member and only theirs',
    'return_books.sql',
    $$SELECT id, returned_on FROM loans WHERE returned_on IS NULL OR returned_on = DATE '2026-11-15'$$,
    $$VALUES (2, NULL::date), (4, NULL), (5, DATE '2026-11-15'), (6, DATE '2026-11-15')$$
);

SELECT dojo.script_eq(
    'an open loan that is not late is closed without a fee',
    'return_books.sql',
    $$SELECT 'fee', loan_id FROM fees UNION ALL SELECT 'returned', id FROM loans WHERE returned_on = DATE '2026-11-15'$$,
    $$VALUES ('fee', 5), ('fee', 6), ('returned', 5), ('returned', 6), ('returned', 13)$$,
    setup => $$INSERT INTO loans VALUES (13, 8, 3, DATE '2026-11-10', DATE '2026-11-24', NULL)$$
);
