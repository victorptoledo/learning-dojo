-- Stage 2 contract: prices.sql creates prices, whose ranges never overlap for a product and
-- currency, price_at(), set_price(), which keeps the history without gaps, and final_price(),
-- which takes off the best discount that applies.

-- The kettle's id, inserting it the first time it is asked for.
CREATE FUNCTION pb_test.kettle() RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    k bigint;
BEGIN
    SELECT id INTO k FROM products WHERE sku = 'KTL-0042';
    IF k IS NULL THEN
        INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle') RETURNING id INTO k;
    END IF;
    RETURN k;
END
$$;

SELECT dojo.script_ok(
    'a product has a price in each currency',
    NULL,
    $$SELECT count(*) = 2 FROM prices$$,
    'a product should be able to have a EUR and a USD price for the same period',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$,
    after => $$INSERT INTO prices (product_id, currency, amount, valid) VALUES
                 (pb_test.kettle(), 'EUR', 29.90, '[2026-01-01, 2026-02-01)'),
                 (pb_test.kettle(), 'USD', 32.50, '[2026-01-01, 2026-02-01)')$$
);

SELECT dojo.script_fails('two prices of a product in one currency cannot overlap', NULL,
    $$INSERT INTO prices (product_id, currency, amount, valid)
      VALUES ((SELECT id FROM products), 'EUR', 25.00, '[2026-01-20, 2026-03-01)')$$, '23P01',
    'January 20 would have two EUR prices: the table must refuse it with an exclusion constraint',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               INSERT INTO prices (product_id, currency, amount, valid)
               VALUES (pb_test.kettle(), 'EUR', 29.90, '[2026-01-01, 2026-02-01)')$$);

SELECT dojo.script_fails('a price with no end overlaps everything after its start', NULL,
    $$INSERT INTO prices (product_id, currency, amount, valid)
      VALUES ((SELECT id FROM products), 'EUR', 25.00, '[2027-06-01, 2027-07-01)')$$, '23P01',
    'a price valid "until further notice" from 2026 overlaps June 2027',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               INSERT INTO prices (product_id, currency, amount, valid)
               VALUES (pb_test.kettle(), 'EUR', 29.90, '[2026-01-01,)')$$);

SELECT dojo.script_ok(
    'ranges that only touch do not overlap',
    NULL,
    $$SELECT count(*) = 2 FROM prices$$,
    '[Jan, Feb) and [Feb, Mar) only touch: both should be accepted',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$,
    after => $$INSERT INTO prices (product_id, currency, amount, valid) VALUES
                 (pb_test.kettle(), 'EUR', 29.90, '[2026-01-01, 2026-02-01)'),
                 (pb_test.kettle(), 'EUR', 27.90, '[2026-02-01, 2026-03-01)')$$
);

SELECT dojo.script_ok(
    'other products do not conflict',
    NULL,
    $$SELECT count(*) = 2 FROM prices$$,
    'two different products may have prices for the same period',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$,
    after => $$INSERT INTO products (sku, name) VALUES ('MUG-0001', 'Mug');
               INSERT INTO prices (product_id, currency, amount, valid) VALUES
                 (pb_test.kettle(), 'EUR', 29.90, '[2026-01-01, 2026-02-01)'),
                 ((SELECT id FROM products WHERE sku = 'MUG-0001'), 'EUR', 7.50, '[2026-01-01, 2026-02-01)')$$
);

SELECT dojo.script_fails('a range must include its start and exclude its end', NULL,
    $$INSERT INTO prices (product_id, currency, amount, valid)
      VALUES (pb_test.kettle(), 'EUR', 29.90, '(2026-01-01, 2026-02-01]')$$, '23514',
    'valid must be [start, end)',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$);

SELECT dojo.script_fails('a range must have a start', NULL,
    $$INSERT INTO prices (product_id, currency, amount, valid)
      VALUES (pb_test.kettle(), 'EUR', 29.90, tstzrange(NULL, '2026-02-01'))$$, '23514',
    'a price that was valid "since forever" has no start',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$);

SELECT dojo.script_fails('an empty range is refused', NULL,
    $$INSERT INTO prices (product_id, currency, amount, valid)
      VALUES (pb_test.kettle(), 'EUR', 29.90, tstzrange('2026-01-01', '2026-01-01'))$$, '23514',
    'an empty range is a price that is never valid',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$);

SELECT dojo.script_fails('a price is positive', NULL,
    $$INSERT INTO prices (product_id, currency, amount, valid)
      VALUES (pb_test.kettle(), 'EUR', 0, '[2026-01-01,)')$$, '23514',
    'an amount of 0 is refused',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$);

SELECT dojo.script_fails('a price belongs to a product', NULL,
    $$INSERT INTO prices (product_id, currency, amount, valid)
      VALUES (-1, 'EUR', 29.90, '[2026-01-01,)')$$, '23503',
    'product -1 does not exist',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$);

SELECT dojo.script_eq(
    'price_at finds the price of a moment',
    NULL,
    $$SELECT price_at(k, 'EUR', '2025-12-31 23:59'), price_at(k, 'EUR', '2026-01-01'),
             price_at(k, 'EUR', '2026-01-31 23:59:59'), price_at(k, 'EUR', '2026-02-01'),
             price_at(k, 'EUR', '2030-01-01'), price_at(k, 'USD', '2026-01-15')
      FROM (SELECT id AS k FROM products) AS p$$,
    $$VALUES (NULL::numeric, 29.90, 29.90, 27.90, 27.90, NULL::numeric)$$,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               INSERT INTO prices (product_id, currency, amount, valid) VALUES
                 (pb_test.kettle(), 'EUR', 29.90, '[2026-01-01, 2026-02-01)'),
                 (pb_test.kettle(), 'EUR', 27.90, '[2026-02-01,)')$$
);

SELECT dojo.script_eq(
    'set_price keeps the history without gaps',
    NULL,
    $$SELECT amount, lower(valid), upper(valid) FROM prices ORDER BY lower(valid)$$,
    $$VALUES (29.90, '2026-01-01'::timestamptz, '2026-03-01'::timestamptz),
             (24.90, '2026-03-01', '2026-03-15'),
             (27.90, '2026-03-15', NULL)$$,
    ordered => true,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$,
    after => $$SELECT set_price(pb_test.kettle(), 'EUR', 29.90, '2026-01-01');
               SELECT set_price((SELECT id FROM products), 'EUR', 24.90, '2026-03-01');
               SELECT set_price((SELECT id FROM products), 'EUR', 27.90, '2026-03-15')$$
);

SELECT dojo.script_eq(
    'set_price touches only its product and currency',
    NULL,
    $$SELECT p.sku::text, pr.currency::text, pr.amount, upper(pr.valid)
      FROM prices pr JOIN products p ON p.id = pr.product_id ORDER BY 1, 2, 4 NULLS LAST$$,
    $$VALUES ('KTL-0042', 'EUR', 29.90, '2026-03-01'::timestamptz),
             ('KTL-0042', 'EUR', 24.90, NULL),
             ('KTL-0042', 'USD', 32.50, NULL),
             ('MUG-0001', 'EUR', 7.50, NULL)$$,
    ordered => true,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               INSERT INTO products (sku, name) VALUES ('MUG-0001', 'Mug');
               SELECT set_price(pb_test.kettle(), 'USD', 32.50, '2026-01-01');
               SELECT set_price((SELECT id FROM products WHERE sku = 'KTL-0042'), 'EUR', 29.90, '2026-01-01');
               SELECT set_price((SELECT id FROM products WHERE sku = 'MUG-0001'), 'EUR', 7.50, '2026-01-01')$$,
    after => $$SELECT set_price((SELECT id FROM products WHERE sku = 'KTL-0042'), 'EUR', 24.90, '2026-03-01')$$
);

SELECT dojo.script_eq(
    'set_price at the start of a price replaces it',
    NULL,
    $$SELECT amount, lower(valid), upper(valid) FROM prices ORDER BY lower(valid)$$,
    $$VALUES (29.90, '2026-01-01'::timestamptz, '2026-03-01'::timestamptz),
             (22.90, '2026-03-01', NULL)$$,
    ordered => true,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql')$$,
    after => $$SELECT set_price(pb_test.kettle(), 'EUR', 29.90, '2026-01-01');
               SELECT set_price((SELECT id FROM products), 'EUR', 24.90, '2026-03-01');
               SELECT set_price((SELECT id FROM products), 'EUR', 22.90, '2026-03-01')$$
);

SELECT dojo.script_fails('set_price refuses to slip in before a later price', NULL,
    $$SELECT set_price((SELECT id FROM products), 'EUR', 25.00, '2026-02-01')$$, '22023',
    'there is a price from March 1st on: a price from February 1st must be refused',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT set_price(pb_test.kettle(), 'EUR', 29.90, '2026-01-01');
               SELECT set_price((SELECT id FROM products), 'EUR', 24.90, '2026-03-01')$$);

SELECT dojo.script_ok(
    'a refused set_price changes nothing',
    NULL,
    $$SELECT count(*) = 2 AND count(*) FILTER (WHERE upper(valid) IS NULL AND amount = 24.90) = 1 FROM prices$$,
    'after the refused set_price, the history should still be 29.90 until March and 24.90 after',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT set_price(pb_test.kettle(), 'EUR', 29.90, '2026-01-01');
               SELECT set_price((SELECT id FROM products), 'EUR', 24.90, '2026-03-01')$$,
    after => $$DO $do$ BEGIN
                   PERFORM set_price((SELECT id FROM products), 'EUR', 25.00, '2026-02-01');
               EXCEPTION WHEN invalid_parameter_value THEN NULL;
               END $do$ $$
);

-- A kettle at 29.90 EUR from 2026-01-01 on; its id.
CREATE FUNCTION pb_test.priced_kettle() RETURNS bigint
LANGUAGE plpgsql AS $$
DECLARE
    k bigint;
BEGIN
    INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle') RETURNING id INTO k;
    INSERT INTO prices (product_id, currency, amount, valid) VALUES (k, 'EUR', 29.90, '[2026-01-01,)');
    RETURN k;
END
$$;

SELECT dojo.script_eq(
    'final_price without discounts',
    NULL,
    $$SELECT final_price(k, 'EUR', 1, '2026-06-01'), final_price(k, 'EUR', 4, '2026-06-01'),
             final_price(k, 'USD', 1, '2026-06-01'), final_price(k, 'EUR', 1, '2025-06-01')
      FROM (SELECT id AS k FROM products) AS p$$,
    $$VALUES (29.90, 119.60, NULL::numeric, NULL::numeric)$$,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT pb_test.priced_kettle()$$
);

SELECT dojo.script_eq(
    'final_price takes the best discount that applies',
    NULL,
    $$SELECT q, final_price((SELECT id FROM products), 'EUR', q, '2026-06-01')
      FROM generate_series(1, 4) AS q$$,
    $$VALUES (1, 26.91), (2, 53.82), (3, 76.25), (4, 101.66)$$,
    ordered => true,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT pb_test.priced_kettle();
               INSERT INTO discounts (product_id, rule) VALUES
                 ((SELECT id FROM products), '{"kind": "percent", "value": 10}'),
                 ((SELECT id FROM products), '{"kind": "bulk", "min_qty": 3, "value": 15}')$$
);

SELECT dojo.script_eq(
    'discounts never add up',
    NULL,
    $$SELECT q, final_price((SELECT id FROM products), 'EUR', q, '2026-06-01')
      FROM generate_series(1, 6) AS q$$,
    $$VALUES (1, 29.90), (2, 59.80), (3, 80.73), (4, 107.64), (5, 104.65), (6, 125.58)$$,
    ordered => true,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT pb_test.priced_kettle();
               INSERT INTO discounts (product_id, rule) VALUES
                 ((SELECT id FROM products), '{"kind": "bulk", "min_qty": 3, "value": 10}'),
                 ((SELECT id FROM products), '{"kind": "bulk", "min_qty": 5, "value": 30}'),
                 ((SELECT id FROM products), '{"kind": "bulk", "min_qty": 4, "value": 5}')$$
);

SELECT dojo.script_eq(
    'a smaller bulk discount does not beat a bigger percent one',
    NULL,
    $$SELECT final_price((SELECT id FROM products), 'EUR', 3, '2026-06-01')$$,
    $$VALUES (80.73)$$,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT pb_test.priced_kettle();
               INSERT INTO discounts (product_id, rule) VALUES
                 ((SELECT id FROM products), '{"kind": "percent", "value": 10}'),
                 ((SELECT id FROM products), '{"kind": "bulk", "min_qty": 3, "value": 5}')$$
);

SELECT dojo.script_eq(
    'the discounts of other products do not apply',
    NULL,
    $$SELECT final_price((SELECT id FROM products WHERE sku = 'KTL-0042'), 'EUR', 1, '2026-06-01')$$,
    $$VALUES (29.90)$$,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT pb_test.priced_kettle();
               INSERT INTO products (sku, name) VALUES ('MUG-0001', 'Mug');
               INSERT INTO discounts (product_id, rule)
               VALUES ((SELECT id FROM products WHERE sku = 'MUG-0001'), '{"kind": "percent", "value": 50}')$$
);

SELECT dojo.script_eq(
    'final_price uses the price of the moment',
    NULL,
    $$SELECT final_price((SELECT id FROM products), 'EUR', 2, '2026-02-01'),
             final_price((SELECT id FROM products), 'EUR', 2, '2026-03-01')$$,
    $$VALUES (53.82, 44.82)$$,
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT set_price(pb_test.priced_kettle(), 'EUR', 24.90, '2026-03-01');
               INSERT INTO discounts (product_id, rule)
               VALUES ((SELECT id FROM products), '{"kind": "percent", "value": 10}')$$
);

SELECT dojo.script_fails('a quantity below 1 is refused', NULL,
    $$SELECT final_price((SELECT id FROM products), 'EUR', 0, '2026-06-01')$$, '22023',
    'nobody buys 0 kettles',
    setup => $$SELECT pb_test.require('catalog.sql', 'prices.sql');
               SELECT pb_test.priced_kettle()$$);
