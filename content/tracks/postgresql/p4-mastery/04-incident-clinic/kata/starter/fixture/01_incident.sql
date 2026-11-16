-- A shop's database during a bad week. The tests load this file into a fresh PostgreSQL 17.

-- orders.id is an old serial (integer) column, close to its limit.
CREATE TABLE orders (
    id         serial PRIMARY KEY,
    customer   text NOT NULL,
    total      numeric(10, 2) NOT NULL
);
CREATE TABLE order_items (
    order_id integer NOT NULL REFERENCES orders (id),
    line_no  integer NOT NULL,
    sku      text NOT NULL,
    PRIMARY KEY (order_id, line_no)
);
INSERT INTO orders (customer, total) SELECT format('c%s', i), i FROM generate_series(1, 100) AS i;
INSERT INTO order_items (order_id, line_no, sku) SELECT id, 1, 'SKU-1' FROM orders;
SELECT setval('orders_id_seq', 2040109465);              -- 95% of 2,147,483,647

CREATE TABLE tickets (id smallint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, title text);
SELECT setval(pg_get_serial_sequence('tickets', 'id'), 30000);   -- 91.6% of 32,767

CREATE TABLE events (id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY, at timestamptz);
SELECT setval(pg_get_serial_sequence('events', 'id'), 1073741823);   -- 50%

CREATE TABLE invoices (id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, total numeric);
SELECT setval(pg_get_serial_sequence('invoices', 'id'), 2040109465);  -- tiny for a bigint

CREATE SEQUENCE never_used;                                -- last_value is NULL until first use

-- What pg_stat_statements showed this morning, exported before it was reset.
CREATE TABLE statements_snapshot (
    queryid         bigint PRIMARY KEY,
    query           text NOT NULL,
    calls           bigint NOT NULL,
    total_exec_time double precision NOT NULL,   -- milliseconds
    rows            bigint NOT NULL,
    shared_blks_hit bigint NOT NULL,
    shared_blks_read bigint NOT NULL
);
INSERT INTO statements_snapshot VALUES
    (101, 'SELECT * FROM orders WHERE customer = $1', 9000000, 1800000, 9000000, 89000000, 1000000),
    (102, 'SELECT count(*) FROM order_items WHERE sku = $1', 1200, 960000, 1200, 2000000, 38000000),
    (103, 'UPDATE orders SET total = $1 WHERE id = $2', 400000, 120000, 400000, 1600000, 0),
    (104, 'SELECT * FROM events WHERE at > $1 ORDER BY at LIMIT $2', 50000, 600000, 2500000, 30000000, 0),
    (105, 'INSERT INTO events (at) VALUES ($1)', 2000000, 20000, 2000000, 8000000, 0),
    (106, 'SELECT pg_sleep($1)', 10, 100000, 10, 0, 0);
