-- An online shop's catalogue: 100,000 products and 200,000 reviews, generated the same way
-- on every check (setseed). The shop's old indexes are at the bottom; 02_vacuum.sql then
-- vacuums the tables, as autovacuum would have done long ago in production.

-- The dojo's server commits asynchronously; this session waits for each commit, so the
-- vacuum can mark every page all-visible right away.
SET synchronous_commit = on;

CREATE TABLE categories (
    id   integer PRIMARY KEY,
    name text NOT NULL
);

CREATE TABLE products (
    id          bigint PRIMARY KEY,
    sku         text NOT NULL,
    name        text NOT NULL,
    category_id integer NOT NULL REFERENCES categories,
    brand       text NOT NULL,
    price       numeric(10, 2) NOT NULL,
    stock       integer NOT NULL,
    active      boolean NOT NULL,
    created_at  timestamptz NOT NULL
);

CREATE TABLE reviews (
    id         bigint PRIMARY KEY,
    product_id bigint NOT NULL REFERENCES products,
    rating     integer NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at timestamptz NOT NULL
);

-- Where the next page of "newest products" starts: the last row of the page before.
CREATE TABLE page_cursor (
    created_at timestamptz NOT NULL,
    id         bigint NOT NULL
);

INSERT INTO categories
SELECT i, 'Category ' || lpad(i::text, 2, '0')
FROM generate_series(1, 40) AS i;

SELECT setseed(0.42);

INSERT INTO products
SELECT i,
       chr(65 + i % 26) || chr(65 + (i / 26) % 26) || '-' || lpad(i::text, 6, '0'),
       'Product ' || i,
       1 + floor(random() * 40)::integer,
       (ARRAY['Acme', 'Bolt', 'Cobalt', 'Dune', 'Ember', 'Fjord', 'Granite', 'Harbor',
              'Iris', 'Juniper', 'Kestrel', 'Lumen', 'Meridian', 'Nimbus', 'Onyx', 'Pike',
              'Quartz', 'Ridge', 'Summit', 'Tundra'])[1 + floor(random() * 20)::integer],
       round((5 + random() * 495)::numeric, 2),
       CASE WHEN random() < 0.02 THEN floor(random() * 5)::integer
            ELSE 5 + floor(random() * 200)::integer END,
       random() < 0.9,
       timestamptz '2024-01-01 00:00:00+00' + (i / 3) * interval '15 minutes'
FROM generate_series(1, 100000) AS i;

INSERT INTO reviews
SELECT i,
       1 + floor(random() * 100000)::bigint,
       1 + floor(random() * 5)::integer,
       timestamptz '2025-01-01 00:00:00+00' + floor(random() * 525600) * interval '1 minute'
FROM generate_series(1, 200000) AS i;

INSERT INTO page_cursor VALUES (timestamptz '2027-01-01 00:00:00+00', 0);

-- The shop's indexes so far. Some of them earn their keep less than others.
CREATE INDEX products_category_idx ON products (category_id);
CREATE INDEX products_active_idx ON products (active);
CREATE INDEX products_id_idx ON products (id);
CREATE INDEX reviews_rating_idx ON reviews (rating);
