-- The shop of this kata. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE customers (
    id   integer PRIMARY KEY,
    name text NOT NULL
);

CREATE TABLE products (
    id          integer PRIMARY KEY,
    name        text NOT NULL UNIQUE,
    price_cents integer NOT NULL CHECK (price_cents >= 0)
);

CREATE TABLE orders (
    id          integer PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers (id),
    placed_at   timestamptz NOT NULL
);

-- The price is copied at checkout: a later price change must not rewrite old orders.
CREATE TABLE order_items (
    order_id         integer NOT NULL REFERENCES orders (id),
    product_id       integer NOT NULL REFERENCES products (id),
    quantity         integer NOT NULL CHECK (quantity > 0),
    unit_price_cents integer NOT NULL CHECK (unit_price_cents >= 0),
    PRIMARY KEY (order_id, product_id)
);

-- What the "My orders" screen asked for. The API binds these as query parameters; in this
-- kata they come from the single row of this table, and the tests change it.
CREATE TABLE page_request (
    customer_id     integer NOT NULL,
    page_size       integer NOT NULL,
    after_placed_at timestamptz,  -- the cursor: both NULL for the first page
    after_id        integer
);

INSERT INTO customers (id, name) VALUES (1, 'Ada'), (2, 'Linus');

INSERT INTO products (id, name, price_cents) VALUES
    (1, 'Mug', 1200),
    (2, 'Tea', 450),
    (3, 'Notebook', 800),
    (4, 'Pen', 150);

INSERT INTO orders (id, customer_id, placed_at) VALUES
    (1, 1, '2026-01-05 10:00:00+00'),
    (2, 1, '2026-01-08 09:30:00+00'),
    (3, 2, '2026-01-09 12:00:00+00'),
    (4, 1, '2026-01-12 18:00:00+00'),
    (5, 1, '2026-01-12 18:00:00+00'),
    (6, 1, '2026-01-15 08:15:00+00');

INSERT INTO order_items (order_id, product_id, quantity, unit_price_cents) VALUES
    (1, 1, 1, 1200), (1, 2, 2, 450),
    (2, 3, 1, 800),
    (3, 4, 3, 150),
    (4, 2, 1, 450), (4, 4, 2, 150),
    (5, 1, 2, 1100),
    (6, 3, 2, 800), (6, 4, 1, 150), (6, 1, 1, 1200);

INSERT INTO page_request (customer_id, page_size, after_placed_at, after_id) VALUES (1, 3, NULL, NULL);
