-- The tables of this kata. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE products (
    id       integer PRIMARY KEY,
    name     text NOT NULL,
    category text NOT NULL,
    price    numeric(8, 2) NOT NULL,
    stock    integer NOT NULL
);

INSERT INTO products (id, name, category, price, stock) VALUES
    (1, 'Bamboo Sword', 'training', 24.90, 12),
    (2, 'Cotton Belt', 'apparel', 9.50, 40),
    (3, 'Practice Gi', 'apparel', 59.00, 5),
    (4, 'Focus Mitt', 'training', 19.99, 0),
    (5, 'Grip Tape', 'accessories', 4.20, 100),
    (6, 'Water Bottle', 'accessories', 12.00, 25),
    (7, 'Jump Rope', 'training', 7.75, 18),
    (8, 'Mouth Guard', 'protection', 12.00, 30),
    (9, 'Shin Guards', 'protection', 34.50, 8),
    (10, 'Kata Handbook', 'books', 19.99, 3);
