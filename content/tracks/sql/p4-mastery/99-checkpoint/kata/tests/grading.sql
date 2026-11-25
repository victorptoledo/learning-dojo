-- migrate.sql
SELECT dojo.script_ok(
    'stores the fees as exact numbers with cents',
    'migrate.sql',
    $$SELECT data_type = 'numeric' AND numeric_scale = 2 FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = 'plans' AND column_name = 'monthly_fee'$$,
    'plans.monthly_fee must be numeric(10, 2): a float cannot hold 0.10 exactly'
);

SELECT dojo.script_eq(
    'keeps every fee',
    'migrate.sql',
    $$SELECT id, monthly_fee FROM plans ORDER BY id$$,
    $$VALUES (1, 9.90), (2, 29.90), (3, 99.99), (4, 4.95), (5, 19.99)$$,
    ordered => true
);

SELECT dojo.script_ok(
    'drops the column of plan ids',
    'migrate.sql',
    $$SELECT NOT EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_schema = 'public' AND table_name = 'customers' AND column_name = 'plan_ids')$$,
    'customers.plan_ids is still there: once subscriptions holds the plans, drop it, or the two will drift apart'
);

SELECT dojo.script_fails(
    'rejects a subscription to a plan that does not exist',
    'migrate.sql',
    $$INSERT INTO subscriptions (customer_id, plan_id) VALUES (7, 99)$$, '23503',
    'subscriptions.plan_id must reference plans'
);

SELECT dojo.script_fails(
    'rejects a subscription of a customer that does not exist',
    'migrate.sql',
    $$INSERT INTO subscriptions (customer_id, plan_id) VALUES (99, 1)$$, '23503',
    'subscriptions.customer_id must reference customers'
);

SELECT dojo.script_fails(
    'rejects the same plan twice for one customer',
    'migrate.sql',
    $$INSERT INTO subscriptions (customer_id, plan_id) VALUES (1, 2)$$, '23505',
    'a customer pays for a plan once: make (customer_id, plan_id) the primary key'
);

SELECT dojo.script_fails(
    'rejects a subscription without a plan',
    'migrate.sql',
    $$INSERT INTO subscriptions (customer_id, plan_id) VALUES (7, NULL)$$, '23502',
    'both columns of subscriptions are required'
);

-- close_month.sql starts from a database migrated by the tests, not by migrate.sql.
\set migrated 'ALTER TABLE plans ALTER COLUMN monthly_fee TYPE numeric(10, 2) USING round(monthly_fee::numeric, 2); CREATE TABLE subscriptions (customer_id integer NOT NULL REFERENCES customers (id), plan_id integer NOT NULL REFERENCES plans (id), PRIMARY KEY (customer_id, plan_id)); INSERT INTO subscriptions VALUES (1, 2), (1, 4), (2, 1), (3, 3), (3, 4), (3, 5), (5, 1), (6, 2), (8, 1); ALTER TABLE customers DROP COLUMN plan_ids'

SELECT dojo.script_eq(
    'invoices November in Sao Paulo time',
    'close_month.sql',
    $$SELECT customer_id, amount FROM invoices WHERE period = DATE '2026-11-01' ORDER BY customer_id$$,
    $$VALUES (1, 39.85), (2, 9.90), (3, 145.13), (4, 0.60), (5, 9.90), (6, 29.90), (8, 9.90)$$,
    ordered => true,
    setup => :'migrated'
);

SELECT dojo.script_ok(
    'sends no invoice to a customer with nothing to pay',
    'close_month.sql',
    $$SELECT NOT EXISTS (SELECT 1 FROM invoices WHERE customer_id = 7)$$,
    'Ken has no subscription and no usage in November, so he gets no invoice',
    setup => :'migrated'
);

SELECT dojo.script_eq(
    'leaves the invoices of other months alone',
    'close_month.sql',
    $$SELECT count(*), sum(amount) FROM invoices WHERE period <> DATE '2026-11-01'$$,
    $$VALUES (7, 421.98)$$,
    setup => :'migrated'
);

SELECT dojo.script_eq(
    'can run twice without failing or duplicating',
    'close_month.sql',
    $$SELECT customer_id, amount FROM invoices WHERE period = DATE '2026-11-01' ORDER BY customer_id$$,
    $$VALUES (1, 39.85), (2, 9.90), (3, 145.13), (4, 0.60), (5, 9.90), (6, 29.90), (8, 9.90)$$,
    ordered => true,
    setup => :'migrated',
    after => $$SELECT dojo.run_file('close_month.sql')$$
);

SELECT dojo.script_eq(
    'a run after late usage updates the invoice',
    'close_month.sql',
    $$SELECT customer_id, amount FROM invoices WHERE period = DATE '2026-11-01' AND customer_id IN (2, 4) ORDER BY customer_id$$,
    $$VALUES (2, 11.90), (4, 0.60)$$,
    ordered => true,
    setup => :'migrated',
    after => $$INSERT INTO usage VALUES (8, 2, '2026-11-25 12:00+00', 100); SELECT dojo.run_file('close_month.sql')$$
);

-- search_customers.sql
SELECT dojo.script_eq(
    'finds a customer by part of the name, ignoring case',
    'search_customers.sql',
    $$SELECT * FROM search_customers('LOVE')$$,
    $$VALUES (1, 'Ada Lovelace')$$
);

SELECT dojo.script_eq(
    'finds a name with a quote in it',
    'search_customers.sql',
    $$SELECT * FROM search_customers('o''h')$$,
    $$VALUES (7, 'Ken O''Hara')$$
);

SELECT dojo.script_eq(
    'a term cannot change the query',
    'search_customers.sql',
    $$SELECT * FROM search_customers($t$' OR true OR name ILIKE '$t$)$$,
    $$SELECT 1, 'x' WHERE false$$
);

SELECT dojo.script_eq(
    'a percent sign in the term is just a character',
    'search_customers.sql',
    $$SELECT * FROM search_customers('100%')$$,
    $$VALUES (8, '100% Coffee Ltd')$$
);

SELECT dojo.script_eq(
    'an underscore in the term is just a character',
    'search_customers.sql',
    $$SELECT * FROM search_customers('a_a')$$,
    $$SELECT 1, 'x' WHERE false$$
);

-- latest_invoices.sql
SELECT dojo.script_eq(
    'returns the three latest invoices of each customer of the page',
    'latest_invoices.sql',
    $$SELECT * FROM latest_invoices(ARRAY[3, 1])$$,
    $$VALUES (1, DATE '2026-10-01', 40.85), (1, DATE '2026-09-01', 36.10), (1, DATE '2026-08-01', 34.85),
             (3, DATE '2026-10-01', 135.43), (3, DATE '2026-09-01', 130.00)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'keeps a customer without invoices',
    'latest_invoices.sql',
    $$SELECT * FROM latest_invoices(ARRAY[4, 2])$$,
    $$VALUES (2, DATE '2026-10-01', 9.90), (4, NULL::date, NULL::numeric)$$,
    ordered => true
);

SELECT dojo.script_eq(
    'ignores unknown and repeated ids',
    'latest_invoices.sql',
    $$SELECT * FROM latest_invoices(ARRAY[99, 2, 2])$$,
    $$VALUES (2, DATE '2026-10-01', 9.90)$$,
    ordered => true
);

SELECT dojo.script_ok(
    'answers the page with one query, not a loop',
    'latest_invoices.sql',
    $$SELECT bool_and(prosrc !~* '\m(loop|foreach)\M') FROM pg_proc WHERE proname = 'latest_invoices'$$,
    'latest_invoices loops: a loop that runs a query per id is the N+1 again, inside the database. Join the ids (= ANY or unnest) and use LATERAL for the latest three'
);
