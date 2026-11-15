-- A shop with enough rows for indexes to matter: 50 000 customers and 200 000 orders.
-- The data is generated, always the same. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE customers (
    id         bigint PRIMARY KEY,
    email      text NOT NULL,
    country    char(2) NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE TABLE orders (
    id          bigint PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES customers (id),
    status      text NOT NULL,
    placed_at   timestamptz NOT NULL,
    total       numeric(10, 2) NOT NULL
);

-- Emails were typed by people: some have capital letters.
INSERT INTO customers (id, email, country, created_at)
SELECT i,
       CASE WHEN i % 3 = 0 THEN 'User' ELSE 'user' END || i || '@example.com',
       (ARRAY['PT', 'BR', 'US', 'DE', 'FI'])[1 + i % 5],
       TIMESTAMPTZ '2024-01-01 00:00+00' + i * INTERVAL '10 minutes'
FROM generate_series(1, 50000) AS i;

-- 500 customers have placed orders, 400 each. About 1% of the orders are pending, 10%
-- cancelled and the rest paid; one order every 157 seconds through 2025.
INSERT INTO orders (id, customer_id, status, placed_at, total)
SELECT i,
       1 + (i * 7919) % 500,
       CASE WHEN i % 100 = 0 THEN 'pending' WHEN i % 10 = 1 THEN 'cancelled' ELSE 'paid' END,
       TIMESTAMPTZ '2025-01-01 00:00+00' + i * INTERVAL '157 seconds',
       (i % 500) + 0.99
FROM generate_series(1, 200000) AS i;

-- The only index besides the primary keys.
CREATE INDEX orders_placed_at ON orders (placed_at);
