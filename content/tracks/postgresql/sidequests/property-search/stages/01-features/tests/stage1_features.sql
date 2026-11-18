-- Stage 1 contract: pool_and_sea.sql and top_floor_a.sql return the right rows and, with the
-- indexes of indexes.sql, stay within their buffer budgets.

SELECT dojo.query_eq(
    'pool and sea view with pets allowed',
    'pool_and_sea.sql',
    $$SELECT id, title, price FROM properties
      WHERE 'pool' = ANY (features) AND 'sea-view' = ANY (features) AND (attrs ->> 'pets')::boolean
      ORDER BY price, id$$,
    ordered => true
);

SELECT dojo.query_eq(
    'energy A on floor 20',
    'top_floor_a.sql',
    $$SELECT id, city, price FROM properties
      WHERE attrs ->> 'energy' = 'A' AND attrs ->> 'floor' = '20'
      ORDER BY id$$,
    ordered => true
);

SELECT dojo.script_ok(
    'indexes.sql creates an index on properties',
    NULL,
    $$SELECT count(*) > 1 FROM pg_index WHERE indrelid = 'properties'::regclass$$,
    'indexes.sql should create at least one index on properties',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'pool and sea view reads at most 100 buffers',
    NULL,
    $$SELECT search_test.within('pool_and_sea.sql', 100)$$,
    'pool_and_sea.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'energy A on floor 20 reads at most 200 buffers',
    NULL,
    $$SELECT search_test.within('top_floor_a.sql', 200)$$,
    'top_floor_a.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);
