SELECT dojo.query_eq(
    'lists every product by name',
    'all_products.sql',
    $$VALUES ('Bamboo Sword', 24.90), ('Cotton Belt', 9.50), ('Focus Mitt', 19.99),
             ('Grip Tape', 4.20), ('Jump Rope', 7.75), ('Kata Handbook', 19.99),
             ('Mouth Guard', 12.00), ('Practice Gi', 59.00), ('Shin Guards', 34.50),
             ('Water Bottle', 12.00)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'lists cheap products in stock cheapest first',
    'cheap_products.sql',
    $$VALUES ('Grip Tape', 4.20), ('Jump Rope', 7.75), ('Cotton Belt', 9.50),
             ('Mouth Guard', 12.00), ('Water Bottle', 12.00), ('Kata Handbook', 19.99)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a product at exactly 20 is not cheap',
    'cheap_products.sql',
    $$VALUES ('Grip Tape', 4.20), ('Jump Rope', 7.75), ('Cotton Belt', 9.50),
             ('Mouth Guard', 12.00), ('Water Bottle', 12.00), ('Kata Handbook', 19.99)$$,
    ordered => true,
    setup => $$INSERT INTO products VALUES (11, 'Hand Wraps', 'protection', 20.00, 9)$$
);

SELECT dojo.query_eq(
    'equal prices are ordered by name',
    'cheap_products.sql',
    $$VALUES ('Ankle Tape', 1.00), ('Zip Bag', 1.00)$$,
    ordered => true,
    setup => $$DELETE FROM products;
                INSERT INTO products VALUES (20, 'Zip Bag', 'accessories', 1.00, 1),
                                            (21, 'Ankle Tape', 'accessories', 1.00, 1)$$
);

SELECT dojo.query_eq(
    'a new category appears once in its place',
    'categories.sql',
    $$VALUES ('accessories'), ('apparel'), ('books'), ('food'), ('protection'), ('training')$$,
    ordered => true,
    setup => $$INSERT INTO products VALUES (11, 'Energy Bar', 'food', 2.00, 50),
                                           (12, 'Protein Bar', 'food', 2.50, 50)$$
);

SELECT dojo.query_eq(
    'no products gives no categories',
    'categories.sql',
    $$SELECT 'x' WHERE false$$,
    setup => $$DELETE FROM products$$
);
