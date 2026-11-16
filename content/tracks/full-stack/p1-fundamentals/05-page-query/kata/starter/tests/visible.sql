-- Each test runs one of your files and compares its rows with the expected ones.
SELECT dojo.query_eq(
    'the first page has the three newest orders of Ada',
    'order_page.sql',
    $$VALUES (6, TIMESTAMPTZ '2026-01-15 08:15:00+00', 4, 2950),
             (5, TIMESTAMPTZ '2026-01-12 18:00:00+00', 2, 2200),
             (4, TIMESTAMPTZ '2026-01-12 18:00:00+00', 3, 750)$$,
    ordered => true
);
