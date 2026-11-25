-- The shop of Checkpoint 3: a catalogue with JSON attributes, its stock and its orders.

-- 20 000 products, so that the planner has a reason to use (or skip) an index.
-- attrs holds brand, color, sizes and, for one product in ten, "promo": true.
CREATE TABLE products (
    id      integer PRIMARY KEY,
    sku     text NOT NULL UNIQUE,
    name    text NOT NULL,
    price   numeric(10, 2) NOT NULL CHECK (price >= 0),
    attrs   jsonb NOT NULL DEFAULT '{}',
    version integer NOT NULL DEFAULT 1
);

INSERT INTO products (id, sku, name, price, attrs, version)
SELECT n,
       'SKU-' || lpad(n::text, 5, '0'),
       'Product ' || n,
       round(1 + (n * 37 % 10000) / 100.0, 2),
       CASE WHEN n = 42
            THEN '{"brand": "Brand 42", "color": "green", "promo": true, "sizes": ["S", "M"]}'::jsonb
            ELSE jsonb_build_object(
                     'brand', 'Brand ' || lpad((n % 50)::text, 2, '0'),
                     'color', CASE WHEN n % 97 = 0 THEN 'red'
                                   ELSE (ARRAY['black', 'white', 'grey', 'blue', 'green'])[1 + n % 5] END,
                     'sizes', CASE WHEN n % 3 = 0 THEN '["S", "M", "L"]'::jsonb ELSE '["M"]'::jsonb END)
                 || CASE WHEN n % 10 = 0 THEN '{"promo": true}'::jsonb ELSE '{}'::jsonb END
       END,
       CASE WHEN n = 42 THEN 3 ELSE 1 END
FROM generate_series(1, 20000) AS n;

-- What is on the shelf and how much of it is promised to orders that are not shipped yet.
CREATE TABLE stock (
    product_id integer PRIMARY KEY REFERENCES products (id),
    on_hand    integer NOT NULL CHECK (on_hand >= 0),
    reserved   integer NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    CHECK (reserved <= on_hand)
);

INSERT INTO stock (product_id, on_hand, reserved)
SELECT id,
       CASE WHEN id = 42 THEN 5 ELSE 10 + id % 7 END,
       CASE WHEN id = 42 THEN 2 ELSE id % 3 END
FROM products;

-- Each order keeps its lines as a JSON array: [{"sku": ..., "qty": ..., "price": ...}].
-- price is what the customer paid for one unit, which may differ from today's catalogue price.
CREATE TABLE orders (
    id        integer PRIMARY KEY,
    customer  text NOT NULL,
    placed_at timestamptz NOT NULL,
    items     jsonb NOT NULL
);

INSERT INTO orders VALUES
    (501, 'ada',   '2026-11-03 10:00+00',
     '[{"sku": "SKU-00042", "qty": 2, "price": 19.90}, {"sku": "SKU-00007", "qty": 1, "price": 5.50}]'),
    (502, 'grace', '2026-11-15 18:30+00', '[{"sku": "SKU-00007", "qty": 3, "price": 5.00}]'),
    (503, 'linus', '2026-10-31 23:59+00', '[{"sku": "SKU-00042", "qty": 5, "price": 19.90}]'),
    (504, 'ada',   '2026-11-30 23:59:59+00',
     '[{"sku": "SKU-99999", "qty": 1, "price": 12.00}, {"sku": "SKU-00042", "qty": 1, "price": 21.00}]'),
    (505, 'grace', '2026-12-01 00:00+00', '[{"sku": "SKU-00007", "qty": 4, "price": 5.50}]'),
    (506, 'linus', '2026-11-20 09:00+00', '[]'),
    (507, 'barbara', '2026-11-21 14:00+00', '[{"sku": "SKU-00100", "qty": 3, "price": 2.00}]');
