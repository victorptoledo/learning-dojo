-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.query_eq(
    'finds the tables autovacuum should be vacuuming',
    'vacuum_due.sql',
    $$VALUES ('countries', 120, 100, false),
             ('events', 40000000, 25000050, false),
             ('orders', 450000, 400050, true),
             ('sessions', 30000, 15000, false)$$,
    ordered => true
);
