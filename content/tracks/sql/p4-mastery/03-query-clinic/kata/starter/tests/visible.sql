-- Each test runs one of your files and compares its rows with the expected ones.
SELECT dojo.query_eq(
    'lists the customers who referred nobody',
    'no_referrals.sql',
    $$VALUES ('Alan Turing'), ('Barbara Liskov'), ('Dennis Ritchie'), ('Ken Thompson'),
             ('Margaret Hamilton')$$,
    ordered => true
);
