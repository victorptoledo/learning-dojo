-- Each test runs one of your files and compares its rows with the expected ones.
-- ordered => true means the order of the rows matters too.
SELECT dojo.query_eq(
    'lists every category once in alphabetical order',
    'categories.sql',
    $$VALUES ('accessories'), ('apparel'), ('books'), ('protection'), ('training')$$,
    ordered => true
);
