-- Each test runs one of your files and compares its rows with the expected ones.
SELECT dojo.query_eq(
    'claims the due email job with the highest priority',
    'claim.sql',
    $$VALUES (3, 1)$$
);
