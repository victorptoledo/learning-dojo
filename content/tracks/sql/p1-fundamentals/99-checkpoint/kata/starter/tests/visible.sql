-- Each test runs one of your files and compares its rows with the expected ones.
SELECT dojo.query_eq(
    'lists the books on the shelf',
    'available_books.sql',
    $$VALUES ('I, Robot'), ('Kindred'), ('Stories of Your Life and Others'), ('The Dispossessed')$$,
    ordered => true
);
