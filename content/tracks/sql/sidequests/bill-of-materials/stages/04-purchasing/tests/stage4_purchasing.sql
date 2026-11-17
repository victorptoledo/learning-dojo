-- Stage 4 contract: current_prices.sql, price_changes.sql and purchase_plan.sql, on 2026-11-01.

SELECT dojo.query_eq(
    'the cheapest current supplier of every part',
    'current_prices.sql',
    $$VALUES ('AXLE', 'Bolt & Co', 3.04), ('BAR-DROP', 'Alpine Parts', 38.75),
             ('BAR-FLAT', 'Cycleworks', 18.38), ('BATTERY', 'Alpine Parts', 0.66),
             ('BB-CUP', 'Delta Supply', 6.54), ('BEARING', 'Bolt & Co', 2.21),
             ('CABLE', 'Bolt & Co', 1.29), ('CALIPER', 'Bolt & Co', 12.82),
             ('CASS-11', 'Delta Supply', 42.78), ('CHAIN-11', 'Delta Supply', 21.78),
             ('CHAIN-8', 'Alpine Parts', 8.94), ('CRANK-50', 'Bolt & Co', 72.52),
             ('DERAILLEUR', 'Cycleworks', 37.05), ('HUB-SHELL', 'Delta Supply', 11.29),
             ('LAMP-F', 'Bolt & Co', 12.59), ('PAD', 'Cycleworks', 1.92),
             ('PAINT', 'Cycleworks', 11.40), ('PEDAL', 'Alpine Parts', 9.46),
             ('RIM-700', 'Alpine Parts', 22.58), ('SADDLE', 'Bolt & Co', 19.90),
             ('SP-SPOKE', 'Bolt & Co', 0.40), ('SPROCKET', 'Alpine Parts', 5.48),
             ('TB-ALU', 'Alpine Parts', 97.46), ('TB-STEEL', 'Alpine Parts', 65.17),
             ('TIRE-700', 'Cycleworks', 18.87), ('TUBE-700', 'Bolt & Co', 4.47)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a price starting on the day counts, a later one does not',
    'current_prices.sql',
    $$VALUES ('BB-CUP', 'Delta Supply', 6.54), ('TB-ALU', 'Delta Supply', 90.00)$$,
    ordered => true,
    setup => $$INSERT INTO prices VALUES (20, 4, 90.00, '2026-11-01'), (20, 3, 80.00, '2026-11-02');
                DELETE FROM prices WHERE part_id NOT IN (20, 29)$$
);

SELECT dojo.query_eq(
    'a supplier''s old price does not count once it changed',
    'current_prices.sql',
    $$VALUES ('CALIPER', 'Alpine Parts', 18.12)$$,
    setup => $$DELETE FROM prices WHERE part_id <> 38 OR supplier_id <> 1$$
);

SELECT dojo.query_eq(
    'each price with the one before it',
    'price_changes.sql',
    $$VALUES ('BB-CUP', 'Alpine Parts', date '2026-04-01', 6.90, NULL, NULL),
             ('BB-CUP', 'Alpine Parts', date '2026-07-01', 7.78, 6.90, 12.8),
             ('BB-CUP', 'Alpine Parts', date '2026-12-01', 8.70, 7.78, 11.8),
             ('BB-CUP', 'Cycleworks', date '2026-12-01', 6.28, NULL, NULL),
             ('BB-CUP', 'Delta Supply', date '2026-01-15', 6.48, NULL, NULL),
             ('BB-CUP', 'Delta Supply', date '2026-07-01', 6.54, 6.48, 0.9),
             ('CHAIN-8', 'Alpine Parts', date '2026-07-01', 8.94, NULL, NULL),
             ('CHAIN-8', 'Bolt & Co', date '2026-04-01', 9.36, NULL, NULL),
             ('CHAIN-8', 'Bolt & Co', date '2026-07-01', 8.94, 9.36, -4.5),
             ('CHAIN-8', 'Bolt & Co', date '2026-12-01', 8.20, 8.94, -8.3)$$,
    ordered => true,
    setup => $$DELETE FROM prices WHERE part_id NOT IN (29, 33)$$
);

SELECT dojo.query_eq(
    'the previous price is the one just before in time',
    'price_changes.sql',
    $$VALUES ('PEDAL', 'Alpine Parts', date '2026-02-01', 10.00, NULL, NULL),
             ('PEDAL', 'Alpine Parts', date '2026-04-01', 9.46, 10.00, -5.4),
             ('PEDAL', 'Alpine Parts', date '2026-08-01', 11.35, 9.46, 20.0)$$,
    ordered => true,
    setup => $$INSERT INTO prices VALUES (37, 1, 11.35, '2026-08-01'), (37, 1, 10.00, '2026-02-01');
                DELETE FROM prices WHERE part_id <> 37$$
);

SELECT dojo.query_eq(
    'plans the purchase for twenty road bikes',
    'purchase_plan.sql',
    $$VALUES ('CRANK-50', 20, 'Bolt & Co', 72.52, 1450.40, 1450.40),
             ('TIRE-700', 40, 'Cycleworks', 18.87, 754.80, 2205.20),
             ('CHAIN-11', 20, 'Delta Supply', 21.78, 435.60, 2640.80),
             ('RIM-700', 15, 'Alpine Parts', 22.58, 338.70, 2979.50),
             ('PEDAL', 30, 'Alpine Parts', 9.46, 283.80, 3263.30),
             ('BEARING', 90, 'Bolt & Co', 2.21, 198.90, 3462.20),
             ('TUBE-700', 40, 'Bolt & Co', 4.47, 178.80, 3641.00),
             ('SP-SPOKE', 380, 'Bolt & Co', 0.40, 152.00, 3793.00),
             ('PAD', 40, 'Cycleworks', 1.92, 76.80, 3869.80)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'stock changes what to buy',
    'purchase_plan.sql',
    $$VALUES ('TIRE-700', 40, 'Cycleworks', 18.87, 754.80, 754.80),
             ('PEDAL', 30, 'Alpine Parts', 9.46, 283.80, 1038.60),
             ('BEARING', 90, 'Bolt & Co', 2.21, 198.90, 1237.50),
             ('TUBE-700', 40, 'Bolt & Co', 4.47, 178.80, 1416.30),
             ('PAD', 40, 'Cycleworks', 1.92, 76.80, 1493.10),
             ('SP-SPOKE', 30, 'Bolt & Co', 0.40, 12.00, 1505.10)$$,
    ordered => true,
    setup => $$UPDATE stock SET on_hand = 20 WHERE part_id IN (30, 32);
                UPDATE stock SET on_hand = 40 WHERE part_id = 22;
                UPDATE stock SET on_hand = 1250 WHERE part_id = 23$$
);

SELECT dojo.query_eq(
    'a part without stock or without a price still shows',
    'purchase_plan.sql',
    $$VALUES ('CRANK-50', 20, 'Bolt & Co', 72.52, 1450.40, 1450.40),
             ('TIRE-700', 40, 'Cycleworks', 18.87, 754.80, 2205.20),
             ('CHAIN-11', 20, 'Delta Supply', 21.78, 435.60, 2640.80),
             ('RIM-700', 15, 'Alpine Parts', 22.58, 338.70, 2979.50),
             ('PEDAL', 30, 'Alpine Parts', 9.46, 283.80, 3263.30),
             ('BEARING', 90, 'Bolt & Co', 2.21, 198.90, 3462.20),
             ('TUBE-700', 40, 'Bolt & Co', 4.47, 178.80, 3641.00),
             ('SP-SPOKE', 380, 'Bolt & Co', 0.40, 152.00, 3793.00),
             ('CABLE', 40, 'Bolt & Co', 1.29, 51.60, 3844.60),
             ('PAD', 40, NULL, NULL, NULL, 3844.60)$$,
    ordered => true,
    setup => $$DELETE FROM stock WHERE part_id = 40;
                DELETE FROM prices WHERE part_id = 39$$
);
