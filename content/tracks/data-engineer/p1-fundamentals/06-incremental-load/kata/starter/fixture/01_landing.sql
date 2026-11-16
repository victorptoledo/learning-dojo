-- The landing table: every extract of the orders API is appended here as it arrives.
-- It is append-only and may hold several versions of an order, exact duplicates (a retried
-- extract) and old versions delivered again.
CREATE TABLE raw_orders (
    order_id     integer NOT NULL,
    status       text NOT NULL,
    amount       numeric(10, 2) NOT NULL,
    updated_at   timestamptz NOT NULL,   -- when the order changed in the source
    _ingested_at timestamptz NOT NULL    -- when the row landed here
);

-- The clean table the analysts query: one row per order, its latest version.
CREATE TABLE orders (
    order_id   integer PRIMARY KEY,
    status     text NOT NULL,
    amount     numeric(10, 2) NOT NULL,
    updated_at timestamptz NOT NULL
);

-- How far each load has read the landing table.
CREATE TABLE load_state (
    table_name text PRIMARY KEY,
    watermark  timestamptz NOT NULL
);

INSERT INTO load_state VALUES ('orders', '-infinity');

-- The first extract, landed on 2026-11-01 at 01:00.
INSERT INTO raw_orders VALUES
    (1, 'placed', 10.00, '2026-10-31 09:00+00', '2026-11-01 01:00+00'),
    (1, 'paid',   10.00, '2026-10-31 10:00+00', '2026-11-01 01:00+00'),
    (2, 'placed', 25.00, '2026-10-31 11:00+00', '2026-11-01 01:00+00'),
    (2, 'placed', 25.00, '2026-10-31 11:00+00', '2026-11-01 01:00+00'),
    (3, 'placed',  7.50, '2026-10-31 12:00+00', '2026-11-01 01:00+00');
