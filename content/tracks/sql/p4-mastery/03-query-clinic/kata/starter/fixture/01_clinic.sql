-- The shop of the query clinic. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE customers (
    id          integer PRIMARY KEY,
    name        text NOT NULL,
    referred_by integer REFERENCES customers (id)      -- NULL: came on their own
);

CREATE TABLE orders (
    id          integer PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers (id),
    placed_at   timestamptz NOT NULL,
    total       numeric(10, 2) NOT NULL
);

CREATE TABLE tickets (
    id          integer PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers (id),
    opened_at   timestamptz NOT NULL
);

CREATE INDEX orders_placed_id ON orders (placed_at, id);
CREATE INDEX orders_customer ON orders (customer_id);
CREATE INDEX tickets_customer ON tickets (customer_id);

INSERT INTO customers (id, name, referred_by) VALUES
    (1, 'Ada Lovelace', NULL),
    (2, 'Grace Hopper', 1),
    (3, 'Linus Torvalds', NULL),
    (4, 'Margaret Hamilton', 2),
    (5, 'Alan Turing', 1),
    (6, 'Barbara Liskov', NULL),
    (7, 'Dennis Ritchie', 3),
    (8, 'Ken Thompson', NULL);

-- 3000 orders from customers 1 to 6, one every 47 minutes from 2026-01-01 (UTC).
INSERT INTO orders (id, customer_id, placed_at, total)
SELECT i,
       1 + i % 6,
       TIMESTAMPTZ '2026-01-01 00:00+00' + i * INTERVAL '47 minutes',
       (i % 97) + 0.50
FROM generate_series(1, 3000) AS i;

INSERT INTO tickets (id, customer_id, opened_at) VALUES
    (1, 1, '2026-01-10 09:00+00'),
    (2, 1, '2026-02-11 10:00+00'),
    (3, 2, '2026-01-15 11:00+00'),
    (4, 3, '2026-03-01 12:00+00'),
    (5, 3, '2026-03-02 12:00+00'),
    (6, 3, '2026-03-03 12:00+00'),
    (7, 7, '2026-03-05 08:00+00');
