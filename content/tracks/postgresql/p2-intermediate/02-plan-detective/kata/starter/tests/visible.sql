-- Each test runs one of your files and checks the rows or the state it leaves behind.
SELECT dojo.query_eq(
    'reads the status frequencies from the statistics',
    'common_status.sql',
    $$VALUES ('paid', 0.6), ('shipped', 0.25), ('new', 0.1), ('cancelled', 0.04), ('refunded', 0.01)$$,
    ordered => true
);
