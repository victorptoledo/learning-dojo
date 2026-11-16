-- A type 2 customer dimension: each row is one version of a customer, valid from
-- valid_from (inclusive) to valid_to (exclusive). The current version ends on 9999-12-31.
CREATE TABLE dim_customer (
    customer_key integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id  integer NOT NULL,
    name         text NOT NULL,
    city         text NOT NULL,
    segment      text,                   -- NULL while sales has not classified the customer
    valid_from   date NOT NULL,
    valid_to     date NOT NULL,
    is_current   boolean NOT NULL,
    CHECK (valid_from < valid_to)
);

-- At most one current version per customer.
CREATE UNIQUE INDEX dim_customer_one_current ON dim_customer (customer_id) WHERE is_current;

INSERT INTO dim_customer (customer_id, name, city, segment, valid_from, valid_to, is_current)
VALUES
    (1, 'Ada Lovelace', 'London', 'retail', '2026-01-01', '2026-06-01', false),
    (1, 'Ada Lovelace', 'Cambridge', 'retail', '2026-06-01', '9999-12-31', true),
    (2, 'Grace Hopper', 'New York', NULL, '2026-01-01', '9999-12-31', true),
    (3, 'Linus Torvalds', 'Helsinki', 'enterprise', '2026-01-01', '9999-12-31', true);

-- Today's full extract of the CRM: one row per customer it knows.
CREATE TABLE customer_snapshot (
    customer_id   integer PRIMARY KEY,
    name          text NOT NULL,
    city          text NOT NULL,
    segment       text,
    snapshot_date date NOT NULL
);

INSERT INTO customer_snapshot VALUES
    (1, 'Ada Lovelace', 'Cambridge', 'retail', '2026-11-01'),
    (2, 'Grace Hopper', 'New York', 'enterprise', '2026-11-01'),
    (3, 'Linus B. Torvalds', 'Helsinki', 'enterprise', '2026-11-01'),
    (4, 'Margaret Hamilton', 'Boston', 'retail', '2026-11-01');

-- Orders, to report on with the customer as they were when they ordered.
CREATE TABLE fact_orders (
    order_id    integer PRIMARY KEY,
    customer_id integer NOT NULL,
    order_date  date NOT NULL,
    amount      numeric(10, 2) NOT NULL
);

INSERT INTO fact_orders VALUES
    (1, 1, '2026-03-10', 100.00),
    (2, 1, '2026-07-01', 50.00),
    (3, 2, '2026-05-05', 70.00),
    (4, 3, '2026-02-02', 200.00),
    (5, 2, '2026-11-02', 30.00);
