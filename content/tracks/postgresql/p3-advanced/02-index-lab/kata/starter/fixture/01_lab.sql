-- Six tables, each waiting for the right kind of index. Only primary keys exist so far.
-- The tests load this file into a fresh PostgreSQL 17.

-- 200,000 events, one every 10 seconds, always appended in time order.
CREATE TABLE events (
    id        bigint PRIMARY KEY,
    at        timestamptz NOT NULL,
    device_id integer NOT NULL,
    kind      text NOT NULL
);
INSERT INTO events (id, at, device_id, kind)
SELECT i, TIMESTAMPTZ '2026-10-23 00:00+00' + i * INTERVAL '10 seconds', 1 + (i * 7919) % 5000,
       (ARRAY['click', 'view', 'error'])[1 + i % 3]
FROM generate_series(1, 200000) AS i;

-- 20,000 products with tags and free-form attributes.
CREATE TABLE products (
    id    bigint PRIMARY KEY,
    name  text NOT NULL,
    tags  text[] NOT NULL,
    attrs jsonb NOT NULL
);
INSERT INTO products (id, name, tags, attrs)
SELECT i, format('Product %s', i),
       CASE WHEN i % 500 = 0 THEN ARRAY['outdoor', 'sale'] ELSE ARRAY['indoor'] END,
       jsonb_build_object('brand', CASE WHEN i % 400 = 0 THEN 'Kestrel' ELSE format('Brand %s', i % 97) END,
                          'weight_g', 100 + i % 900)
FROM generate_series(1, 20000) AS i;

-- 20,000 users whose emails were typed with any capitalisation.
CREATE TABLE users (
    id    bigint PRIMARY KEY,
    email text NOT NULL
);
INSERT INTO users (id, email)
SELECT i, CASE WHEN i = 4242 THEN 'Ana.Silva@Example.com' ELSE format('User%s@Example.com', i) END
FROM generate_series(1, 20000) AS i;

-- 100,000 jobs, of which only 100 are still pending.
CREATE TABLE jobs (
    id     bigint PRIMARY KEY,
    status text NOT NULL,
    run_at timestamptz NOT NULL
);
INSERT INTO jobs (id, status, run_at)
SELECT i, CASE WHEN i % 1000 = 0 THEN 'pending' ELSE 'done' END,
       TIMESTAMPTZ '2026-11-15 12:00+00' - (i % 977) * INTERVAL '1 minute'
FROM generate_series(1, 100000) AS i;

-- 20,000 customers, searched by any fragment of their name.
CREATE TABLE customers (
    id   bigint PRIMARY KEY,
    name text NOT NULL
);
INSERT INTO customers (id, name)
SELECT i, CASE WHEN i % 2000 = 0 THEN format('Maria Silva %s', i) ELSE format('Customer %s', i) END
FROM generate_series(1, 20000) AS i;

-- 100,000 orders; the account page shows a customer's order totals.
CREATE TABLE orders (
    id          bigint PRIMARY KEY,
    customer_id bigint NOT NULL,
    total       numeric(10, 2) NOT NULL,
    notes       text
);
INSERT INTO orders (id, customer_id, total, notes)
SELECT i, 1 + i % 20000, (i % 500) + 0.99, repeat('x', 100)
FROM generate_series(1, 100000) AS i;

-- Sets the visibility map, so index-only scans can skip the table.
VACUUM orders;
