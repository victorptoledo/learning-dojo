-- Addresses and orders of a shop, and a few plans captured by auto_explain in production.
-- The tests load this file into a fresh PostgreSQL 17; every table has at most 30,000 rows,
-- so ANALYZE reads all of them and the statistics are exact.
CREATE TABLE addresses (
    id          bigint PRIMARY KEY,
    customer_id bigint NOT NULL,
    city        text NOT NULL,
    country     text NOT NULL
);

-- 30 cities in 5 countries: the city decides the country.
INSERT INTO addresses (id, customer_id, city, country)
SELECT i, i,
       (ARRAY['Lisbon', 'Porto', 'Braga', 'Faro', 'Coimbra', 'Evora',
              'Madrid', 'Seville', 'Valencia', 'Bilbao', 'Malaga', 'Zaragoza',
              'Paris', 'Lyon', 'Nantes', 'Lille', 'Nice', 'Bordeaux',
              'Berlin', 'Munich', 'Hamburg', 'Cologne', 'Leipzig', 'Dresden',
              'Rome', 'Milan', 'Turin', 'Naples', 'Genoa', 'Bologna'])[1 + i % 30],
       (ARRAY['PT', 'ES', 'FR', 'DE', 'IT'])[1 + (i % 30) / 6]
FROM generate_series(0, 29999) AS i;

CREATE TABLE orders (
    id          bigint PRIMARY KEY,
    customer_id bigint NOT NULL,
    status      text NOT NULL,
    total       numeric(10, 2) NOT NULL
);

-- 60% paid, 25% shipped, 10% new, 4% cancelled, 1% refunded.
INSERT INTO orders (id, customer_id, status, total)
SELECT i, 1 + i % 30000,
       CASE WHEN i % 100 < 60 THEN 'paid'
            WHEN i % 100 < 85 THEN 'shipped'
            WHEN i % 100 < 95 THEN 'new'
            WHEN i % 100 < 99 THEN 'cancelled'
            ELSE 'refunded' END,
       (i % 500) + 0.99
FROM generate_series(0, 29999) AS i;

-- Plans that auto_explain logged as JSON (EXPLAIN (ANALYZE, FORMAT JSON)), trimmed to the
-- fields you need. "Plan Rows" and "Actual Rows" are both per loop.
CREATE TABLE captured_plans (
    id    integer PRIMARY KEY,
    query text NOT NULL,
    plan  jsonb NOT NULL
);

INSERT INTO captured_plans (id, query, plan) VALUES
(1, 'orders of the customers of one city',
 '[{"Plan": {"Node Type": "Nested Loop", "Plan Rows": 12, "Actual Rows": 144630, "Actual Loops": 1,
    "Plans": [
      {"Node Type": "Seq Scan", "Relation Name": "addresses", "Plan Rows": 12, "Actual Rows": 48210, "Actual Loops": 1},
      {"Node Type": "Index Scan", "Relation Name": "orders", "Index Name": "orders_customer", "Plan Rows": 1, "Actual Rows": 3, "Actual Loops": 48210}
    ]}}]'),
(2, 'revenue of the customers of one country',
 '[{"Plan": {"Node Type": "Hash Join", "Plan Rows": 5000, "Actual Rows": 4800, "Actual Loops": 1,
    "Plans": [
      {"Node Type": "Seq Scan", "Relation Name": "orders", "Plan Rows": 30000, "Actual Rows": 30000, "Actual Loops": 1},
      {"Node Type": "Hash", "Plan Rows": 2500, "Actual Rows": 295, "Actual Loops": 1,
       "Plans": [
         {"Node Type": "Bitmap Heap Scan", "Relation Name": "customers", "Plan Rows": 2500, "Actual Rows": 290, "Actual Loops": 1,
          "Plans": [
            {"Node Type": "Bitmap Index Scan", "Index Name": "customers_country", "Plan Rows": 2600, "Actual Rows": 310, "Actual Loops": 1}
          ]}
       ]}
    ]}}]'),
(3, 'count of the refunds of today',
 '[{"Plan": {"Node Type": "Aggregate", "Plan Rows": 1, "Actual Rows": 1, "Actual Loops": 1,
    "Plans": [
      {"Node Type": "Seq Scan", "Relation Name": "orders", "Plan Rows": 300, "Actual Rows": 0, "Actual Loops": 1}
    ]}}]');
