-- Stage 3 contract: nearest.sql and within_radius.sql return the right rows and, with the GiST
-- index of indexes.sql, stay within their buffer budgets; the nearest ones need no sort.

SELECT dojo.query_eq(
    'the 10 nearest listings',
    'nearest.sql',
    $$SELECT id, round(sqrt((location[0] - 50.5) ^ 2 + (location[1] - 50.5) ^ 2)::numeric, 3)
      FROM properties
      ORDER BY (location[0] - 50.5) ^ 2 + (location[1] - 50.5) ^ 2
      LIMIT 10$$,
    ordered => true
);

SELECT dojo.query_eq(
    'listings with 3 bedrooms within 2 km',
    'within_radius.sql',
    $$SELECT count(*) FROM properties
      WHERE (location[0] - 20) ^ 2 + (location[1] - 80) ^ 2 <= 4 AND bedrooms >= 3$$
);

SELECT dojo.script_ok(
    'the nearest listings read at most 30 buffers',
    NULL,
    $$SELECT search_test.within('nearest.sql', 30)$$,
    'nearest.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'the nearest listings need no sort',
    NULL,
    $$SELECT dojo.plan(dojo.sql_of('nearest.sql')) NOT LIKE '%Sort%'$$,
    'the plan of nearest.sql still sorts: a GiST index can hand the points over nearest first',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'the radius reads at most 200 buffers',
    NULL,
    $$SELECT search_test.within('within_radius.sql', 200)$$,
    'within_radius.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);
