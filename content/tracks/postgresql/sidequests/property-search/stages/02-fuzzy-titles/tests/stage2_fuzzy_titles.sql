-- Stage 2 contract: title_contains.sql and did_you_mean.sql return the right rows and, with
-- the trigram index of indexes.sql, stay within their buffer budgets.

SELECT dojo.query_eq(
    'titles containing ribeta in any case',
    'title_contains.sql',
    $$SELECT id, title FROM properties WHERE lower(title) LIKE '%ribeta%' ORDER BY id$$,
    ordered => true
);

-- The 15 best matches of Ribetta in the fixture, computed once with pg_trgm.
SELECT dojo.query_eq(
    'did you mean Ribeta',
    'did_you_mean.sql',
    $$VALUES (7573::bigint, 'Renovated apartment in Ribeta', 0.667),
             (15573, 'Grand studio in Ribeta', 0.667),
             (23573, 'Spacious apartment in Ribeta', 0.667),
             (31573, 'Spacious house in Ribeta', 0.667),
             (39573, 'Classic house in Ribeta', 0.667),
             (47573, 'Renovated apartment in Ribeta', 0.667),
             (55573, 'Stylish apartment in Ribeta', 0.667),
             (63573, 'Sunny house in Ribeta', 0.667),
             (71573, 'Peaceful studio in Ribeta', 0.667),
             (79573, 'Classic apartment in Ribeta', 0.667),
             (87573, 'Cozy house in Ribeta', 0.667),
             (95573, 'Spacious studio in Ribeta', 0.667),
             (2773, 'Grand studio in Ribete', 0.625),
             (10773, 'Sunny apartment in Ribete', 0.625),
             (18773, 'Modern house in Ribete', 0.625)$$,
    ordered => true
);

SELECT dojo.script_ok(
    'indexes.sql creates pg_trgm',
    NULL,
    $$SELECT EXISTS (SELECT FROM pg_extension WHERE extname = 'pg_trgm')$$,
    'indexes.sql should create the pg_trgm extension',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'a title substring reads at most 50 buffers',
    NULL,
    $$SELECT search_test.within('title_contains.sql', 50)$$,
    'title_contains.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);

SELECT dojo.script_ok(
    'did you mean reads at most 100 buffers',
    NULL,
    $$SELECT search_test.within('did_you_mean.sql', 100)$$,
    'did_you_mean.sql is over its budget',
    setup => $$SELECT search_test.require('indexes.sql')$$
);
