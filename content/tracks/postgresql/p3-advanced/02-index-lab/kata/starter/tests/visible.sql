-- Each test runs one of your files and checks the rows or the plans it leads to.
SELECT dojo.query_eq(
    'finds the user whatever the case',
    'login.sql',
    $$VALUES (4242, 'Ana.Silva@Example.com')$$
);
