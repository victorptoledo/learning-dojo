-- Each test runs star.sql on the fixture's OLTP tables, then checks the star.
SELECT dojo.script_ok(
    'builds one fact row per order line that was not cancelled',
    'star.sql',
    $$SELECT count(*) = 8 FROM fact_sales$$,
    'fact_sales needs one row per order line, without the lines of cancelled orders'
);
