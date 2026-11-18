-- Stage 1 contract: catalog.sql creates the sku domain, the product_status and currency enums,
-- the products table and the discounts table, whose rules hold whatever writes to them.

SELECT dojo.script_ok(
    'a product needs only a sku and a name',
    NULL,
    $$SELECT (SELECT status::text = 'draft' AND tags = '{}' AND id IS NOT NULL FROM r)$$,
    'INSERT INTO products (sku, name) VALUES (''KTL-0042'', ''Kettle'') should create a draft product with no tags and an id',
    setup => $$SELECT pb_test.require('catalog.sql')$$,
    after => $$CREATE TEMP TABLE r AS
               WITH i AS (INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle') RETURNING *)
               SELECT id, status, tags::text[] AS tags FROM i$$
);

SELECT dojo.script_ok(
    'sku is a domain',
    NULL,
    $$SELECT EXISTS (SELECT FROM pg_type WHERE typname = 'sku' AND typtype = 'd'
                     AND typnamespace = 'public'::regnamespace)
         AND (SELECT format_type(atttypid, NULL) FROM pg_attribute
              WHERE attrelid = 'products'::regclass AND attname = 'sku') = 'sku'$$,
    'there should be a domain public.sku, and products.sku should be of that type',
    setup => $$SELECT pb_test.require('catalog.sql')$$
);

SELECT dojo.script_fails('the sku domain refuses lowercase letters', NULL,
    $$SELECT 'ktl-0042'::sku$$, '23514', 'a sku has three UPPERCASE letters',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('the sku domain refuses three digits', NULL,
    $$SELECT 'KTL-042'::sku$$, '23514', 'a sku ends in exactly four digits',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('the sku domain refuses anything around a valid sku', NULL,
    $$SELECT 'old KTL-0042'::sku$$, '23514', 'the whole value must be a sku, not just contain one',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('a sku is unique', NULL,
    $$INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Another kettle')$$, '23505',
    'two products cannot share a sku',
    setup => $$SELECT pb_test.require('catalog.sql');
               INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle')$$);

SELECT dojo.script_fails('a blank name is refused', NULL,
    $$INSERT INTO products (sku, name) VALUES ('KTL-0042', '   ')$$, '23514',
    'a name made only of spaces is blank',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('the name is required', NULL,
    $$INSERT INTO products (sku, name) VALUES ('KTL-0042', NULL)$$, '23502',
    'a product needs a name',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_ok(
    'the statuses follow the life of a product',
    NULL,
    $$SELECT enum_range(NULL::product_status)::text[] = '{draft,active,retired}'$$,
    'product_status should be an enum of draft, active and retired, in that order',
    setup => $$SELECT pb_test.require('catalog.sql')$$
);

SELECT dojo.script_fails('an unknown status is refused', NULL,
    $$INSERT INTO products (sku, name, status) VALUES ('KTL-0042', 'Kettle', 'archived')$$, '22P02',
    'archived is not a product_status',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_ok(
    'the currencies',
    NULL,
    $$SELECT enum_range(NULL::currency)::text[] = '{EUR,USD,BRL}'$$,
    'currency should be an enum of EUR, USD and BRL, in that order',
    setup => $$SELECT pb_test.require('catalog.sql')$$
);

SELECT dojo.script_ok(
    'five good tags are accepted',
    NULL,
    $$SELECT (SELECT tags::text[] FROM products WHERE sku = 'TNT-0001') = '{outdoor,size-42,tent,blue,2-person}'$$,
    'a product with the tags {outdoor,size-42,tent,blue,2-person} should be stored as given',
    setup => $$SELECT pb_test.require('catalog.sql')$$,
    after => $$INSERT INTO products (sku, name, tags)
               VALUES ('TNT-0001', 'Tent', '{outdoor,size-42,tent,blue,2-person}')$$
);

SELECT dojo.script_fails('six tags are too many', NULL,
    $$INSERT INTO products (sku, name, tags) VALUES ('TNT-0001', 'Tent', '{a,b,c,d,e,f}')$$, '23514',
    'a product has at most 5 tags',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('a tag cannot repeat', NULL,
    $$INSERT INTO products (sku, name, tags) VALUES ('TNT-0001', 'Tent', '{outdoor,tent,outdoor}')$$, '23514',
    'outdoor appears twice',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('a tag is lowercase', NULL,
    $$INSERT INTO products (sku, name, tags) VALUES ('TNT-0001', 'Tent', '{outdoor,Tent}')$$, '23514',
    'Tent has an uppercase letter',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('a tag has no spaces', NULL,
    $$INSERT INTO products (sku, name, tags) VALUES ('TNT-0001', 'Tent', '{"two person"}')$$, '23514',
    'a tag is made of lowercase letters, digits and dashes only',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('an empty tag is refused', NULL,
    $$INSERT INTO products (sku, name, tags) VALUES ('TNT-0001', 'Tent', '{outdoor,""}')$$, '23514',
    'an empty string is not a tag',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('a NULL tag is refused', NULL,
    $$INSERT INTO products (sku, name, tags) VALUES ('TNT-0001', 'Tent', ARRAY['outdoor', NULL])$$, '23514',
    'NULL is not a tag',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_fails('a later update must follow the tag rules too', NULL,
    $$UPDATE products SET tags = tags || 'tent'::text WHERE sku = 'TNT-0001'$$, '23514',
    'appending a tag the product already has must fail as well',
    setup => $$SELECT pb_test.require('catalog.sql');
               INSERT INTO products (sku, name, tags) VALUES ('TNT-0001', 'Tent', '{tent}')$$);

-- Tries to store a rule: 'ok', or the SQLSTATE it failed with.
CREATE FUNCTION pb_test.try_rule(r text) RETURNS text
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO discounts (product_id, rule)
    VALUES ((SELECT id FROM products WHERE sku = 'KTL-0042'), r::jsonb);
    RETURN 'ok';
EXCEPTION WHEN OTHERS THEN
    RETURN SQLSTATE;
END
$$;

SELECT dojo.script_eq(
    'the two shapes of a rule are accepted',
    NULL,
    $$SELECT r, pb_test.try_rule(r) FROM unnest(ARRAY[
        '{"kind": "percent", "value": 10}',
        '{"kind": "percent", "value": 1}',
        '{"kind": "percent", "value": 90}',
        '{"kind": "bulk", "min_qty": 3, "value": 15}',
        '{"value": 20, "min_qty": 2, "kind": "bulk"}']) AS r$$,
    $$VALUES ('{"kind": "percent", "value": 10}', 'ok'),
             ('{"kind": "percent", "value": 1}', 'ok'),
             ('{"kind": "percent", "value": 90}', 'ok'),
             ('{"kind": "bulk", "min_qty": 3, "value": 15}', 'ok'),
             ('{"value": 20, "min_qty": 2, "kind": "bulk"}', 'ok')$$,
    setup => $$SELECT pb_test.require('catalog.sql');
               INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle')$$
);

SELECT dojo.script_eq(
    'rules of an unknown shape are refused by a check',
    NULL,
    $$SELECT r, pb_test.try_rule(r) FROM unnest(ARRAY[
        '{"kind": "bogo"}',
        '{"kind": "percent"}',
        '{"kind": "percent", "value": "10"}',
        '{"kind": "percent", "value": 12.5}',
        '{"kind": "percent", "value": 0}',
        '{"kind": "percent", "value": 95}',
        '{"kind": "percent", "value": 10, "note": "x"}',
        '{"kind": "bulk", "value": 15}',
        '{"kind": "bulk", "min_qty": 1, "value": 15}',
        '{"kind": "bulk", "min_qty": 2.5, "value": 15}',
        '{"kind": "bulk", "min_qty": "3", "value": 15}',
        '{"kind": "PERCENT", "value": 10}',
        '{"value": 10}',
        '[]',
        '"percent"']) AS r$$,
    $$SELECT r, '23514' FROM unnest(ARRAY[
        '{"kind": "bogo"}',
        '{"kind": "percent"}',
        '{"kind": "percent", "value": "10"}',
        '{"kind": "percent", "value": 12.5}',
        '{"kind": "percent", "value": 0}',
        '{"kind": "percent", "value": 95}',
        '{"kind": "percent", "value": 10, "note": "x"}',
        '{"kind": "bulk", "value": 15}',
        '{"kind": "bulk", "min_qty": 1, "value": 15}',
        '{"kind": "bulk", "min_qty": 2.5, "value": 15}',
        '{"kind": "bulk", "min_qty": "3", "value": 15}',
        '{"kind": "PERCENT", "value": 10}',
        '{"value": 10}',
        '[]',
        '"percent"']) AS r$$,
    setup => $$SELECT pb_test.require('catalog.sql');
               INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle')$$
);

SELECT dojo.script_fails('a rule is required', NULL,
    $$INSERT INTO discounts (product_id, rule) VALUES ((SELECT id FROM products), NULL)$$, '23502',
    'a discount without a rule makes no sense',
    setup => $$SELECT pb_test.require('catalog.sql');
               INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle')$$);

SELECT dojo.script_fails('a discount belongs to a product', NULL,
    $$INSERT INTO discounts (product_id, rule) VALUES (-1, '{"kind": "percent", "value": 10}')$$, '23503',
    'product -1 does not exist',
    setup => $$SELECT pb_test.require('catalog.sql')$$);

SELECT dojo.script_ok(
    'kind is a stored generated column',
    NULL,
    $$SELECT (SELECT attgenerated = 's' AND format_type(atttypid, NULL) = 'text'
              FROM pg_attribute WHERE attrelid = 'discounts'::regclass AND attname = 'kind')
         AND (SELECT array_agg(kind ORDER BY id) = '{percent,bulk}' FROM discounts)$$,
    'discounts.kind should be a text column GENERATED ALWAYS AS (...) STORED holding the rule''s kind',
    setup => $$SELECT pb_test.require('catalog.sql');
               INSERT INTO products (sku, name) VALUES ('KTL-0042', 'Kettle')$$,
    after => $$INSERT INTO discounts (product_id, rule) VALUES
                 ((SELECT id FROM products), '{"kind": "percent", "value": 10}'),
                 ((SELECT id FROM products), '{"kind": "bulk", "min_qty": 3, "value": 15}')$$
);
