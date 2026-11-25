-- Each test runs one of your files and compares its rows with the expected ones.
-- Product 42 has 5 on hand and 2 reserved: 3 available.
SELECT dojo.query_eq(
    'reserves 2 of the 3 units left of product 42',
    'reserve.sql',
    $$VALUES (42, 1)$$
);
