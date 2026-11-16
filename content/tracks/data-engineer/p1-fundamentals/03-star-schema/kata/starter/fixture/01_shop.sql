-- The shop's operational (OLTP) tables: normalised, one row per business entity.
CREATE TABLE customers (
    id      integer PRIMARY KEY,
    name    text NOT NULL,
    email   text NOT NULL UNIQUE,
    city    text NOT NULL,
    country text NOT NULL
);

CREATE TABLE products (
    id       integer PRIMARY KEY,
    name     text NOT NULL,
    category text NOT NULL,
    price    numeric(10, 2) NOT NULL
);

CREATE TABLE orders (
    id          integer PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers,
    ordered_at  timestamptz NOT NULL,
    status      text NOT NULL CHECK (status IN ('paid', 'shipped', 'cancelled'))
);

CREATE TABLE order_items (
    order_id   integer NOT NULL REFERENCES orders,
    line_no    integer NOT NULL,
    product_id integer NOT NULL REFERENCES products,
    quantity   integer NOT NULL CHECK (quantity > 0),
    unit_price numeric(10, 2) NOT NULL,
    PRIMARY KEY (order_id, line_no)
);

INSERT INTO customers VALUES
    (1, 'Ada Lovelace', 'ada@example.com', 'London', 'GB'),
    (2, 'Grace Hopper', 'grace@example.com', 'New York', 'US'),
    (3, 'Linus Torvalds', 'linus@example.com', 'Helsinki', 'FI'),
    (4, 'Margaret Hamilton', 'margaret@example.com', 'Boston', 'US');

INSERT INTO products VALUES
    (10, 'Keyboard', 'Hardware', 80.00),
    (11, 'Mouse', 'Hardware', 25.00),
    (12, 'IDE licence', 'Software', 120.00),
    (13, 'SQL course', 'Training', 300.00);

INSERT INTO orders VALUES
    (100, 1, '2026-01-15 10:00+00', 'paid'),
    (101, 2, '2026-01-31 23:30+00', 'shipped'),
    (102, 3, '2026-02-01 09:00+00', 'cancelled'),
    (103, 1, '2026-02-14 12:00+00', 'paid'),
    (104, 2, '2026-03-31 18:00+00', 'paid'),
    (105, 3, '2026-04-01 08:00+00', 'shipped');

INSERT INTO order_items VALUES
    (100, 1, 10, 1, 80.00),
    (100, 2, 11, 2, 25.00),
    (101, 1, 12, 1, 110.00),
    (102, 1, 13, 1, 300.00),
    (103, 1, 11, 1, 25.00),
    (103, 2, 13, 1, 300.00),
    (104, 1, 10, 2, 75.00),
    (104, 2, 12, 1, 120.00),
    (105, 1, 13, 2, 280.00);
