-- Stage 1 contract: explode.sql (components of one road bike) and where_used.sql.

SELECT dojo.query_eq(
    'explodes the road bike into its components',
    'explode.sql',
    $$VALUES ('AXLE', 'Hub axle', 2), ('BAR-DROP', 'Drop handlebar', 1),
             ('BB-CUP', 'Bottom bracket cup', 2), ('BEARING', 'Sealed bearing', 6),
             ('CABLE', 'Brake cable', 2), ('CALIPER', 'Brake caliper', 2),
             ('CASS-11', '11-speed cassette', 1), ('CHAIN-11', '11-speed chain', 1),
             ('CRANK-50', '50T crankset', 1), ('DERAILLEUR', 'Rear derailleur', 1),
             ('HUB-SHELL', 'Hub shell', 2), ('PAD', 'Brake pad', 4), ('PAINT', 'Paint kit', 1),
             ('PEDAL', 'Pedal', 2), ('RIM-700', '700c rim', 2), ('SADDLE', 'Saddle', 1),
             ('SP-SPOKE', 'Spoke', 64), ('TB-ALU', 'Aluminium tube set', 1),
             ('TIRE-700', '700c tyre', 2), ('TUBE-700', 'Inner tube', 2)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'quantities multiply down the tree',
    'explode.sql',
    $$VALUES ('AXLE', 'Hub axle', 2), ('BEARING', 'Sealed bearing', 2), ('HUB-SHELL', 'Hub shell', 2),
             ('RIM-700', '700c rim', 2), ('SP-SPOKE', 'Spoke', 72), ('TIRE-700', '700c tyre', 2),
             ('TUBE-700', 'Inner tube', 2)$$,
    ordered => true,
    setup => $$UPDATE bom SET quantity = 36 WHERE parent_id = 5 AND child_id = 23;
                UPDATE bom SET quantity = 1 WHERE parent_id = 6 AND child_id = 27;
                DELETE FROM bom WHERE parent_id = 1 AND child_id <> 5$$
);

SELECT dojo.query_eq(
    'a deeper tree is followed to the bottom',
    'explode.sql',
    $$VALUES ('AXLE', 'Hub axle', 2), ('BEARING', 'Sealed bearing', 4), ('HUB-SHELL', 'Hub shell', 2),
             ('NUT', 'Axle nut', 8), ('RIM-700', '700c rim', 2), ('SP-SPOKE', 'Spoke', 64),
             ('TIRE-700', '700c tyre', 2), ('TUBE-700', 'Inner tube', 2)$$,
    ordered => true,
    setup => $$INSERT INTO parts VALUES (60, 'QR-SET', 'Quick release', 'assembly', NULL),
                                        (61, 'NUT', 'Axle nut', 'component', 0.20);
                INSERT INTO bom VALUES (6, 60, 1), (60, 61, 4);
                DELETE FROM bom WHERE parent_id = 1 AND child_id <> 5$$
);

SELECT dojo.query_eq(
    'finds every part that uses the bearing',
    'where_used.sql',
    $$VALUES ('BB-SET', 1), ('HUB-STD', 1), ('FR-CITY', 2), ('FR-ROAD', 2), ('WH-700', 2),
             ('BK-CITY', 3), ('BK-ROAD', 3)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a part reached at two levels shows the nearest',
    'where_used.sql',
    $$VALUES ('BB-SET', 1), ('BK-ROAD', 1), ('HUB-STD', 1), ('FR-CITY', 2), ('FR-ROAD', 2),
             ('WH-700', 2), ('BK-CITY', 3)$$,
    ordered => true,
    setup => $$INSERT INTO bom VALUES (1, 27, 2)$$
);

SELECT dojo.query_eq(
    'only the parts that still use it',
    'where_used.sql',
    $$VALUES ('BB-SET', 1), ('FR-CITY', 2), ('FR-ROAD', 2), ('BK-CITY', 3), ('BK-ROAD', 3)$$,
    ordered => true,
    setup => $$DELETE FROM bom WHERE parent_id = 6 AND child_id = 27$$
);
