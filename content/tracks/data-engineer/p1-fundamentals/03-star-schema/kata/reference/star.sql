-- A star schema for sales, built from the shop's OLTP tables.
-- Grain of the fact table: one row per order line of an order that was not cancelled.
CREATE TABLE dim_date (
    date_key    integer PRIMARY KEY,           -- YYYYMMDD
    full_date   date NOT NULL UNIQUE,
    year        integer NOT NULL,
    quarter     integer NOT NULL,
    month       integer NOT NULL,
    day_of_week integer NOT NULL               -- ISO: 1 = Monday ... 7 = Sunday
);

CREATE TABLE dim_customer (
    customer_key integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id  integer NOT NULL UNIQUE,      -- the natural key from the source
    name         text NOT NULL,
    city         text NOT NULL,
    country      text NOT NULL
);

CREATE TABLE dim_product (
    product_key integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id  integer NOT NULL UNIQUE,
    name        text NOT NULL,
    category    text NOT NULL
);

CREATE TABLE fact_sales (
    order_id     integer NOT NULL,             -- a degenerate dimension
    line_no      integer NOT NULL,
    date_key     integer NOT NULL REFERENCES dim_date,
    customer_key integer NOT NULL REFERENCES dim_customer,
    product_key  integer NOT NULL REFERENCES dim_product,
    quantity     integer NOT NULL,
    unit_price   numeric(10, 2) NOT NULL,
    revenue      numeric(12, 2) NOT NULL,
    PRIMARY KEY (order_id, line_no)
);

INSERT INTO dim_date (date_key, full_date, year, quarter, month, day_of_week)
SELECT to_char(d, 'YYYYMMDD')::integer, d, extract(year FROM d), extract(quarter FROM d),
       extract(month FROM d), extract(isodow FROM d)
FROM (
    SELECT g::date AS d
    FROM generate_series(
             (SELECT min((ordered_at AT TIME ZONE 'UTC')::date) FROM orders),
             (SELECT max((ordered_at AT TIME ZONE 'UTC')::date) FROM orders),
             interval '1 day'
         ) AS g
) AS days;

INSERT INTO dim_customer (customer_id, name, city, country)
SELECT id, name, city, country FROM customers ORDER BY id;

INSERT INTO dim_product (product_id, name, category)
SELECT id, name, category FROM products ORDER BY id;

INSERT INTO fact_sales (order_id, line_no, date_key, customer_key, product_key,
                        quantity, unit_price, revenue)
SELECT o.id, i.line_no,
       to_char(o.ordered_at AT TIME ZONE 'UTC', 'YYYYMMDD')::integer,
       c.customer_key, p.product_key,
       i.quantity, i.unit_price, i.quantity * i.unit_price
FROM orders AS o
JOIN order_items AS i ON i.order_id = o.id
JOIN dim_customer AS c ON c.customer_id = o.customer_id
JOIN dim_product AS p ON p.product_id = i.product_id
WHERE o.status <> 'cancelled';
