-- The shop of this kata. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE customers (
    id      integer PRIMARY KEY,
    name    text NOT NULL,
    email   text NOT NULL UNIQUE,
    country char(2) NOT NULL
);

CREATE TABLE products (
    id       integer PRIMARY KEY,
    name     text NOT NULL,
    category text NOT NULL,
    price    numeric(8, 2) NOT NULL
);

CREATE TABLE orders (
    id          integer PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers (id),
    placed_at   timestamptz NOT NULL,
    status      text NOT NULL CHECK (status IN ('pending', 'paid', 'cancelled'))
);

-- unit_price is the price paid, which may differ from today's products.price.
CREATE TABLE order_items (
    order_id   integer NOT NULL REFERENCES orders (id),
    product_id integer NOT NULL REFERENCES products (id),
    quantity   integer NOT NULL CHECK (quantity > 0),
    unit_price numeric(8, 2) NOT NULL,
    PRIMARY KEY (order_id, product_id)
);

INSERT INTO customers (id, name, email, country) VALUES
    (1, 'Ada Lovelace', 'ada@example.com', 'GB'),
    (2, 'Grace Hopper', 'grace@example.com', 'US'),
    (3, 'Linus Torvalds', 'linus@example.com', 'FI'),
    (4, 'Margaret Hamilton', 'margaret@example.com', 'US'),
    (5, 'Alan Turing', 'alan@example.com', 'GB'),
    (6, 'Barbara Liskov', 'barbara@example.com', 'US');

INSERT INTO products (id, name, category, price) VALUES
    (1, 'Bamboo Sword', 'training', 24.90),
    (2, 'Cotton Belt', 'apparel', 9.50),
    (3, 'Practice Gi', 'apparel', 59.00),
    (4, 'Focus Mitt', 'training', 19.99),
    (5, 'Grip Tape', 'accessories', 4.20),
    (6, 'Water Bottle', 'accessories', 12.00),
    (7, 'Jump Rope', 'training', 7.75),
    (8, 'Kata Handbook', 'books', 19.99);

INSERT INTO orders (id, customer_id, placed_at, status) VALUES
    (101, 1, '2026-01-05 10:00+00', 'paid'),
    (102, 1, '2026-01-20 18:30+00', 'paid'),
    (103, 2, '2026-01-31 23:50+00', 'paid'),
    (104, 3, '2026-02-02 09:15+00', 'cancelled'),
    (105, 2, '2026-02-14 12:00+00', 'paid'),
    (106, 4, '2026-02-28 16:45+00', 'pending'),
    (107, 1, '2026-03-03 08:00+00', 'paid'),
    (108, 5, '2026-03-10 11:11+00', 'paid'),
    (109, 3, '2026-03-21 20:00+00', 'paid'),
    (110, 2, '2026-04-01 07:30+00', 'cancelled'),
    (111, 5, '2025-12-30 15:00+00', 'paid');

INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES
    (101, 1, 1, 24.90), (101, 2, 2, 9.50),
    (102, 3, 1, 59.00), (102, 5, 3, 4.20),
    (103, 5, 5, 4.20), (103, 6, 2, 12.00), (103, 7, 1, 7.75),
    (104, 3, 2, 59.00),
    (105, 1, 2, 22.00), (105, 5, 2, 4.20),
    (106, 6, 1, 12.00),
    (107, 2, 1, 9.50), (107, 7, 4, 7.75),
    (108, 3, 1, 55.00), (108, 6, 1, 12.00), (108, 5, 1, 4.20),
    (109, 7, 2, 7.75),
    (110, 1, 3, 24.90),
    (111, 2, 3, 9.50);
