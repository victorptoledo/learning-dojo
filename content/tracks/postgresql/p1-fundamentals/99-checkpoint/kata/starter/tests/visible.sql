-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.query_eq(
    'finds the free rooms with a projector',
    'free_rooms.sql',
    $$VALUES (6, 'Evora'), (4, 'Faro')$$,
    ordered => true
);
