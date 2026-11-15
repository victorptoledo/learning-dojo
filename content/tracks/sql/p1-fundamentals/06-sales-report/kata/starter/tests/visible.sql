-- Each test runs one of your files and compares its rows with the expected ones.
SELECT dojo.query_eq(
    'lists the products nobody ordered',
    'never_ordered.sql',
    $$VALUES ('Focus Mitt'), ('Kata Handbook')$$,
    ordered => true
);
