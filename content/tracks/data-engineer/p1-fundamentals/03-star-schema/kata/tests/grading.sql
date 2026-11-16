-- The reports must read the star only: the OLTP tables are emptied after star.sql.
\set star 'SELECT dojo.run_file(''star.sql''); TRUNCATE order_items, orders, products, customers CASCADE'

SELECT dojo.script_ok(
    'creates the four tables with their columns',
    'star.sql',
    $$SELECT count(*) = 23 FROM information_schema.columns
      WHERE table_schema = 'public'
        AND (table_name, column_name) IN (
            ('dim_date', 'date_key'), ('dim_date', 'full_date'), ('dim_date', 'year'),
            ('dim_date', 'quarter'), ('dim_date', 'month'), ('dim_date', 'day_of_week'),
            ('dim_customer', 'customer_key'), ('dim_customer', 'customer_id'),
            ('dim_customer', 'name'), ('dim_customer', 'city'), ('dim_customer', 'country'),
            ('dim_product', 'product_key'), ('dim_product', 'product_id'),
            ('dim_product', 'name'), ('dim_product', 'category'),
            ('fact_sales', 'order_id'), ('fact_sales', 'line_no'), ('fact_sales', 'date_key'),
            ('fact_sales', 'customer_key'), ('fact_sales', 'product_key'),
            ('fact_sales', 'quantity'), ('fact_sales', 'unit_price'), ('fact_sales', 'revenue'))$$,
    'the tables need every column of the README'
);

SELECT dojo.script_ok(
    'dim_date has one row per day from the first to the last order',
    'star.sql',
    $$SELECT count(*) = 77 AND count(DISTINCT full_date) = 77
             AND min(full_date) = '2026-01-15' AND max(full_date) = '2026-04-01'
      FROM dim_date$$,
    'dim_date must cover every day from 2026-01-15 to 2026-04-01, each once'
);

SELECT dojo.script_ok(
    'dim_date keys are YYYYMMDD numbers',
    'star.sql',
    $$SELECT bool_and(date_key = to_char(full_date, 'YYYYMMDD')::integer) FROM dim_date$$,
    'date_key must be the date written as YYYYMMDD, for example 20260214'
);

SELECT dojo.script_eq(
    'dim_date describes each day',
    'star.sql',
    $$SELECT date_key, year, quarter, month, day_of_week FROM dim_date
      WHERE full_date IN ('2026-02-14', '2026-03-31', '2026-04-01') ORDER BY date_key$$,
    $$VALUES (20260214, 2026, 1, 2, 6), (20260331, 2026, 1, 3, 2), (20260401, 2026, 2, 4, 3)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'dim_customer has every customer, also those without orders',
    'star.sql',
    $$SELECT customer_id, name, city, country FROM dim_customer ORDER BY customer_id$$,
    $$VALUES (1, 'Ada Lovelace', 'London', 'GB'), (2, 'Grace Hopper', 'New York', 'US'),
             (3, 'Linus Torvalds', 'Helsinki', 'FI'), (4, 'Margaret Hamilton', 'Boston', 'US')$$,
    ordered => true
);

SELECT dojo.script_eq(
    'dim_product has every product',
    'star.sql',
    $$SELECT product_id, name, category FROM dim_product ORDER BY product_id$$,
    $$VALUES (10, 'Keyboard', 'Hardware'), (11, 'Mouse', 'Hardware'),
             (12, 'IDE licence', 'Software'), (13, 'SQL course', 'Training')$$,
    ordered => true
);

SELECT dojo.script_ok(
    'every dimension row has its own key',
    'star.sql',
    $$SELECT (SELECT count(DISTINCT customer_key) FROM dim_customer) = 4
             AND (SELECT count(DISTINCT product_key) FROM dim_product) = 4$$,
    'customer_key and product_key must identify one row each'
);

SELECT dojo.script_eq(
    'fact rows point at the right dimension rows',
    'star.sql',
    $$SELECT f.order_id, f.line_no, d.full_date, c.name, p.name, f.quantity, f.unit_price, f.revenue
      FROM fact_sales AS f
      JOIN dim_date AS d USING (date_key)
      JOIN dim_customer AS c USING (customer_key)
      JOIN dim_product AS p USING (product_key)
      WHERE f.order_id IN (101, 104)$$,
    $$VALUES (101, 1, DATE '2026-01-31', 'Grace Hopper', 'IDE licence', 1, 110.00, 110.00),
             (104, 1, DATE '2026-03-31', 'Grace Hopper', 'Keyboard', 2, 75.00, 150.00),
             (104, 2, DATE '2026-03-31', 'Grace Hopper', 'IDE licence', 1, 120.00, 120.00)$$
);

SELECT dojo.script_ok(
    'the fact table leaves out cancelled orders and sums to the paid revenue',
    'star.sql',
    $$SELECT sum(revenue) = 1395.00 AND bool_and(order_id <> 102) FROM fact_sales$$,
    'revenue must be quantity times the price paid, without order 102 (cancelled)'
);

SELECT dojo.script_fails(
    'a fact row needs an existing date',
    'star.sql',
    $$INSERT INTO fact_sales (order_id, line_no, date_key, customer_key, product_key,
                             quantity, unit_price, revenue)
      SELECT 900, 1, 19990101, customer_key, product_key, 1, 1, 1 FROM fact_sales LIMIT 1$$,
    '23503', 'fact_sales.date_key must reference dim_date'
);

SELECT dojo.script_fails(
    'a fact row needs an existing customer',
    'star.sql',
    $$INSERT INTO fact_sales (order_id, line_no, date_key, customer_key, product_key,
                             quantity, unit_price, revenue)
      SELECT 900, 1, date_key, -1, product_key, 1, 1, 1 FROM fact_sales LIMIT 1$$,
    '23503', 'fact_sales.customer_key must reference dim_customer'
);

SELECT dojo.script_fails(
    'a fact row needs an existing product',
    'star.sql',
    $$INSERT INTO fact_sales (order_id, line_no, date_key, customer_key, product_key,
                             quantity, unit_price, revenue)
      SELECT 900, 1, date_key, customer_key, -1, 1, 1, 1 FROM fact_sales LIMIT 1$$,
    '23503', 'fact_sales.product_key must reference dim_product'
);

SELECT dojo.script_fails(
    'an order line is in the fact table once',
    'star.sql',
    $$INSERT INTO fact_sales (order_id, line_no, date_key, customer_key, product_key,
                             quantity, unit_price, revenue)
      SELECT order_id, line_no, date_key, customer_key, product_key, 1, 1, 1
      FROM fact_sales LIMIT 1$$,
    '23505', 'the grain is one row per order line: (order_id, line_no) must be unique'
);

SELECT dojo.script_fails(
    'a natural key is in its dimension once',
    'star.sql',
    $$INSERT INTO dim_customer (customer_id, name, city, country) VALUES (1, 'Ada', 'Paris', 'FR')$$,
    '23505', 'dim_customer.customer_id must be unique'
);

SELECT dojo.query_eq(
    'revenue by month and category reads the star',
    'revenue_by_month_category.sql',
    $$VALUES (2026, 1, 'Hardware', 130.00), (2026, 1, 'Software', 110.00),
             (2026, 2, 'Hardware', 25.00), (2026, 2, 'Training', 300.00),
             (2026, 3, 'Hardware', 150.00), (2026, 3, 'Software', 120.00),
             (2026, 4, 'Training', 560.00)$$,
    ordered => true,
    setup => :'star'
);

SELECT dojo.query_eq(
    'revenue by country and quarter counts orders once',
    'revenue_by_country_quarter.sql',
    $$VALUES ('FI', 2026, 2, 1, 560.00), ('GB', 2026, 1, 2, 455.00), ('US', 2026, 1, 2, 380.00)$$,
    ordered => true,
    setup => :'star'
);
