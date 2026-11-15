SELECT dojo.query_eq(
    'reports every customer by revenue',
    'revenue_by_customer.sql',
    $$VALUES ('Ada Lovelace', 3, 156.00), ('Grace Hopper', 2, 105.15),
             ('Alan Turing', 2, 99.70), ('Linus Torvalds', 1, 15.50),
             ('Barbara Liskov', 0, 0), ('Margaret Hamilton', 0, 0)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a new customer without orders gets zeros',
    'revenue_by_customer.sql',
    $$VALUES ('Ada Lovelace', 3, 156.00), ('Grace Hopper', 2, 105.15),
             ('Alan Turing', 2, 99.70), ('Linus Torvalds', 1, 15.50),
             ('Barbara Liskov', 0, 0), ('Dennis Ritchie', 0, 0), ('Margaret Hamilton', 0, 0)$$,
    ordered => true,
    setup => $$INSERT INTO customers VALUES (7, 'Dennis Ritchie', 'dennis@example.com', 'US')$$
);

SELECT dojo.query_eq(
    'cancelled and pending orders bring no revenue',
    'revenue_by_customer.sql',
    $$VALUES ('Ada Lovelace', 3, 156.00), ('Alan Turing', 2, 99.70),
             ('Linus Torvalds', 1, 15.50), ('Barbara Liskov', 0, 0),
             ('Grace Hopper', 0, 0), ('Margaret Hamilton', 0, 0)$$,
    ordered => true,
    setup => $$UPDATE orders SET status = 'cancelled' WHERE id = 103;
                UPDATE orders SET status = 'pending' WHERE id = 105$$
);

SELECT dojo.query_eq(
    'lists the three best sellers',
    'best_sellers.sql',
    $$VALUES ('Grip Tape', 11), ('Jump Rope', 7), ('Cotton Belt', 6)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'best sellers with equal units are ordered by name',
    'best_sellers.sql',
    $$VALUES ('Grip Tape', 11), ('Bamboo Sword', 3), ('Water Bottle', 3)$$,
    ordered => true,
    setup => $$DELETE FROM order_items WHERE product_id IN (2, 7)$$
);

SELECT dojo.query_eq(
    'units of cancelled orders do not count',
    'best_sellers.sql',
    $$VALUES ('Grip Tape', 11), ('Jump Rope', 7), ('Cotton Belt', 6)$$,
    ordered => true,
    setup => $$INSERT INTO order_items VALUES (104, 8, 20, 19.99)$$
);

SELECT dojo.query_eq(
    'a product ordered only in a cancelled order was still ordered',
    'never_ordered.sql',
    $$VALUES ('Kata Handbook')$$,
    ordered => true,
    setup => $$INSERT INTO order_items VALUES (110, 4, 1, 19.99)$$
);

SELECT dojo.query_eq(
    'reports the months of 2026 with revenue of at least 100',
    'monthly_revenue.sql',
    $$VALUES (DATE '2026-01-01', 3, 168.25), (DATE '2026-03-01', 3, 127.20)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a month reaching exactly 100 is included and other years are not',
    'monthly_revenue.sql',
    $$VALUES (DATE '2026-01-01', 3, 168.25), (DATE '2026-02-01', 2, 100.00),
             (DATE '2026-03-01', 3, 127.20)$$,
    ordered => true,
    setup => $$INSERT INTO orders VALUES (112, 6, '2026-02-20 10:00+00', 'paid'),
                                         (113, 6, '2027-01-02 10:00+00', 'paid');
                INSERT INTO order_items VALUES (112, 2, 5, 9.52), (113, 3, 9, 59.00)$$
);
