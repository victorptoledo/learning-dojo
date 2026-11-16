-- A small shop in two schemas. The tests load this file into a fresh PostgreSQL 17.
CREATE SCHEMA shop;
CREATE SCHEMA audit;

CREATE TABLE shop.customers (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      text NOT NULL UNIQUE,
    name       text NOT NULL,
    country    char(2),
    created_at timestamptz NOT NULL DEFAULT '2026-01-01 00:00+00'
);

CREATE TABLE shop.products (
    id    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku   text NOT NULL UNIQUE,
    name  text NOT NULL,
    price numeric(10, 2) NOT NULL CHECK (price >= 0)
);

CREATE TABLE shop.orders (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES shop.customers (id),
    status      text NOT NULL DEFAULT 'new',
    placed_at   timestamptz NOT NULL,
    total       numeric(12, 2),
    notes       text
);
CREATE INDEX orders_customer ON shop.orders (customer_id, placed_at);

CREATE TABLE shop.order_items (
    order_id   bigint NOT NULL REFERENCES shop.orders (id),
    line_no    integer NOT NULL,
    product_id bigint NOT NULL REFERENCES shop.products (id),
    quantity   integer NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, line_no)
);

CREATE TABLE shop.shipments (
    id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id bigint NOT NULL,
    line_no  integer NOT NULL,
    carrier  text NOT NULL,
    FOREIGN KEY (order_id, line_no) REFERENCES shop.order_items (order_id, line_no)
);
-- The key columns in another order: lookups on both columns can still use it.
CREATE INDEX shipments_line_order ON shop.shipments (line_no, order_id);

CREATE TABLE shop.reviews (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id  bigint NOT NULL REFERENCES shop.products (id),
    customer_id bigint NOT NULL REFERENCES shop.customers (id),
    stars       smallint NOT NULL CHECK (stars BETWEEN 1 AND 5),
    body        text
);
CREATE INDEX reviews_customer ON shop.reviews (customer_id);
-- product_id is in this index, but not as its first column.
CREATE INDEX reviews_stars_product ON shop.reviews (stars, product_id);

CREATE TABLE audit.log (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    at         timestamptz NOT NULL,
    table_name text NOT NULL,
    row_id     bigint NOT NULL,
    change     jsonb NOT NULL
);

INSERT INTO shop.customers (email, name, country)
SELECT format('customer%s@example.com', i), format('Customer %s', i), (ARRAY['PT', 'BR', 'DE', 'US'])[1 + i % 4]
FROM generate_series(1, 200) AS i;

INSERT INTO shop.products (sku, name, price)
SELECT format('SKU-%s', lpad(i::text, 4, '0')), format('Product %s', i), (i % 40) + 0.99
FROM generate_series(1, 50) AS i;

INSERT INTO shop.orders (customer_id, status, placed_at, total)
SELECT 1 + i % 200, (ARRAY['new', 'paid', 'shipped'])[1 + i % 3],
       TIMESTAMPTZ '2026-01-01 00:00+00' + i * INTERVAL '1 hour', (i % 300) + 0.50
FROM generate_series(1, 1000) AS i;

INSERT INTO shop.order_items (order_id, line_no, product_id, quantity)
SELECT o, l, 1 + (o * l) % 50, 1 + (o + l) % 4
FROM generate_series(1, 1000) AS o, generate_series(1, 3) AS l;

INSERT INTO shop.shipments (order_id, line_no, carrier)
SELECT o, 1, (ARRAY['DHL', 'CTT', 'UPS'])[1 + o % 3]
FROM generate_series(1, 500) AS o;

-- Long review texts are moved to the table's TOAST storage.
INSERT INTO shop.reviews (product_id, customer_id, stars, body)
SELECT 1 + i % 50, 1 + i % 200, 1 + i % 5, repeat(md5(i::text), 100)
FROM generate_series(1, 300) AS i;

INSERT INTO audit.log (at, table_name, row_id, change)
SELECT TIMESTAMPTZ '2026-01-01 00:00+00' + i * INTERVAL '1 minute', 'orders', 1 + i % 1000,
       jsonb_build_object('status', (ARRAY['new', 'paid', 'shipped'])[1 + i % 3])
FROM generate_series(1, 2000) AS i;
