-- Stage 2 contract: rollup.sql (cost of every assembly) and tree.sql (the road bike, indented).

SELECT dojo.query_eq(
    'rolls the component costs up to every assembly',
    'rollup.sql',
    $$VALUES ('BK-ROAD', 544.00), ('BK-CITY', 401.30), ('DT-ROAD', 187.50), ('FR-ROAD', 112.70),
             ('FR-CITY', 87.70), ('WH-700', 76.90), ('DT-CITY', 63.40), ('LGT-SET', 20.40),
             ('BRK-SET', 19.50), ('HUB-STD', 17.40), ('BB-SET', 15.70)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a cheaper spoke changes every assembly above it',
    'rollup.sql',
    $$VALUES ('BK-ROAD', 550.40), ('BK-CITY', 407.70), ('DT-ROAD', 187.50), ('FR-ROAD', 112.70),
             ('FR-CITY', 87.70), ('WH-700', 80.10), ('DT-CITY', 63.40), ('LGT-SET', 20.40),
             ('BRK-SET', 19.50), ('HUB-STD', 17.40), ('BB-SET', 15.70)$$,
    ordered => true,
    setup => $$UPDATE parts SET unit_cost = 0.50 WHERE sku = 'SP-SPOKE'$$
);

SELECT dojo.query_eq(
    'an empty assembly costs nothing',
    'rollup.sql',
    $$VALUES ('BK-ROAD', 544.00), ('BK-CITY', 401.30), ('DT-ROAD', 187.50), ('FR-ROAD', 112.70),
             ('FR-CITY', 87.70), ('WH-700', 76.90), ('DT-CITY', 63.40), ('LGT-SET', 20.40),
             ('BRK-SET', 19.50), ('HUB-STD', 17.40), ('BB-SET', 15.70), ('KIT-TOOLS', 0)$$,
    ordered => true,
    setup => $$INSERT INTO parts VALUES (70, 'KIT-TOOLS', 'Tool kit', 'assembly', NULL)$$
);

SELECT dojo.query_eq(
    'costs follow a new level of the tree',
    'rollup.sql',
    $$VALUES ('BK-ROAD', 547.20), ('BK-CITY', 404.50), ('DT-ROAD', 187.50), ('FR-ROAD', 112.70),
             ('FR-CITY', 87.70), ('WH-700', 78.50), ('DT-CITY', 63.40), ('LGT-SET', 20.40),
             ('BRK-SET', 19.50), ('HUB-STD', 19.00), ('BB-SET', 15.70), ('QR-SET', 1.60)$$,
    ordered => true,
    setup => $$INSERT INTO parts VALUES (60, 'QR-SET', 'Quick release', 'assembly', NULL),
                                        (61, 'NUT', 'Axle nut', 'component', 0.40);
                INSERT INTO bom VALUES (6, 60, 1), (60, 61, 4)$$
);

SELECT dojo.query_eq(
    'prints the road bike as an indented tree',
    'tree.sql',
    $$VALUES (0, 'BK-ROAD x1'), (1, '  BAR-DROP x1'), (1, '  BRK-SET x2'), (2, '    CABLE x1'),
             (2, '    CALIPER x1'), (2, '    PAD x2'), (1, '  DT-ROAD x1'), (2, '    CASS-11 x1'),
             (2, '    CHAIN-11 x1'), (2, '    CRANK-50 x1'), (2, '    DERAILLEUR x1'),
             (2, '    PEDAL x2'), (1, '  FR-ROAD x1'), (2, '    BB-SET x1'), (3, '      BB-CUP x2'),
             (3, '      BEARING x2'), (2, '    PAINT x1'), (2, '    TB-ALU x1'), (1, '  SADDLE x1'),
             (1, '  WH-700 x2'), (2, '    HUB-STD x1'), (3, '      AXLE x1'), (3, '      BEARING x2'),
             (3, '      HUB-SHELL x1'), (2, '    RIM-700 x1'), (2, '    SP-SPOKE x32'),
             (2, '    TIRE-700 x1'), (2, '    TUBE-700 x1')$$,
    ordered => true
);

SELECT dojo.query_eq(
    'the tree follows changes in the parts',
    'tree.sql',
    $$VALUES (0, 'BK-ROAD x1'), (1, '  WH-700 x2'), (2, '    HUB-STD x1'), (3, '      AXLE x1'),
             (3, '      BEARING x2'), (3, '      HUB-SHELL x1'), (3, '      QR-SET x1'),
             (4, '        NUT x4'), (2, '    RIM-700 x1'), (2, '    SP-SPOKE x28'),
             (2, '    TIRE-700 x1'), (2, '    TUBE-700 x1')$$,
    ordered => true,
    setup => $$INSERT INTO parts VALUES (60, 'QR-SET', 'Quick release', 'assembly', NULL),
                                        (61, 'NUT', 'Axle nut', 'component', 0.40);
                INSERT INTO bom VALUES (6, 60, 1), (60, 61, 4);
                UPDATE bom SET quantity = 28 WHERE parent_id = 5 AND child_id = 23;
                DELETE FROM bom WHERE parent_id = 1 AND child_id <> 5$$
);
