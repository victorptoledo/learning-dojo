-- Each test runs one of your files and compares its rows with the expected ones.
SELECT dojo.query_eq(
    'lists the columns of orders in table order',
    'columns.sql',
    $$VALUES ('id', 'bigint', true),
             ('customer_id', 'bigint', true),
             ('status', 'text', true),
             ('placed_at', 'timestamp with time zone', true),
             ('total', 'numeric(12,2)', false),
             ('notes', 'text', false)$$,
    ordered => true
);
