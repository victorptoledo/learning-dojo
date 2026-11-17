-- Stage 3 contract: cycles.sql and bad_parts.sql. The fixture is clean; the tests break it.

SELECT dojo.query_eq(
    'a clean catalogue has no cycles',
    'cycles.sql',
    $$SELECT NULL::text WHERE false$$
);

SELECT dojo.query_eq(
    'finds a part that contains itself',
    'cycles.sql',
    $$VALUES ('BB-SET')$$,
    setup => $$INSERT INTO bom VALUES (7, 7, 1)$$
);

SELECT dojo.query_eq(
    'finds every part of a longer cycle',
    'cycles.sql',
    $$VALUES ('BK-ROAD'), ('FR-ROAD'), ('WH-700')$$,
    ordered => true,
    setup => $$INSERT INTO bom VALUES (5, 3, 1), (3, 1, 1)$$
);

SELECT dojo.query_eq(
    'parts above or below a cycle are not in it',
    'cycles.sql',
    $$VALUES ('HUB-STD'), ('WH-700')$$,
    ordered => true,
    setup => $$INSERT INTO bom VALUES (6, 5, 1)$$
);

SELECT dojo.query_eq(
    'finds two separate cycles',
    'cycles.sql',
    $$VALUES ('BRK-SET'), ('DT-CITY'), ('HUB-STD'), ('WH-700')$$,
    ordered => true,
    setup => $$INSERT INTO bom VALUES (6, 5, 1), (10, 9, 1), (9, 10, 1)$$
);

SELECT dojo.query_eq(
    'a clean catalogue has no bad parts',
    'bad_parts.sql',
    $$SELECT NULL::text, NULL::text WHERE false$$
);

SELECT dojo.query_eq(
    'reports each problem with its text',
    'bad_parts.sql',
    $$VALUES ('BELL', 'component used nowhere'), ('KIT-TOOLS', 'assembly without parts'),
             ('LAMP-R', 'component without cost'), ('PEDAL', 'component with parts'),
             ('SADDLE', 'component without cost'), ('WH-700', 'assembly with a cost')$$,
    ordered => true,
    setup => $$INSERT INTO parts VALUES (70, 'KIT-TOOLS', 'Tool kit', 'assembly', NULL),
                                        (71, 'BELL', 'Bell', 'component', 3.50);
                UPDATE parts SET unit_cost = NULL WHERE sku IN ('LAMP-R', 'SADDLE');
                UPDATE parts SET unit_cost = 70 WHERE sku = 'WH-700';
                INSERT INTO bom VALUES (37, 23, 2)$$
);

SELECT dojo.query_eq(
    'a part with two problems shows both',
    'bad_parts.sql',
    $$VALUES ('BELL', 'component used nowhere'), ('BELL', 'component without cost')$$,
    ordered => true,
    setup => $$INSERT INTO parts VALUES (71, 'BELL', 'Bell', 'component', NULL)$$
);
