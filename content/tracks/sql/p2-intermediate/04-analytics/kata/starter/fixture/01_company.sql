-- The company of this kata. The tests load this file into a fresh PostgreSQL 17.
CREATE TABLE departments (
    id   integer PRIMARY KEY,
    name text NOT NULL UNIQUE
);

-- manager_id points to another employee; the CEO has none.
CREATE TABLE employees (
    id            integer PRIMARY KEY,
    name          text NOT NULL,
    department_id integer NOT NULL REFERENCES departments (id),
    manager_id    integer REFERENCES employees (id),
    salary        numeric(10, 2) NOT NULL
);

-- One row per day the shop was open (closed days have no row).
CREATE TABLE daily_sales (
    day     date PRIMARY KEY,
    revenue numeric(10, 2) NOT NULL
);

INSERT INTO departments (id, name) VALUES
    (1, 'Engineering'),
    (2, 'Sales'),
    (3, 'Support');

INSERT INTO employees (id, name, department_id, manager_id, salary) VALUES
    (1, 'Ines Moura', 1, NULL, 9800.00),
    (2, 'Bruno Reis', 1, 1, 7200.00),
    (3, 'Carla Dias', 2, 1, 7200.00),
    (4, 'Duarte Lima', 1, 2, 6100.00),
    (5, 'Eva Rocha', 1, 4, 4800.00),
    (6, 'Filipe Sousa', 1, 2, 6100.00),
    (7, 'Gabriela Pinto', 2, 3, 5200.00),
    (8, 'Hugo Matos', 2, 3, 4300.00),
    (9, 'Joana Faria', 3, 1, 3900.00),
    (10, 'Luis Neves', 3, 9, 3100.00),
    (11, 'Marta Gomes', 1, 6, 4800.00);

INSERT INTO daily_sales (day, revenue) VALUES
    ('2026-01-28', 300.00),
    ('2026-01-29', 420.00),
    ('2026-01-31', 380.00),
    ('2026-02-02', 150.00),
    ('2026-02-03', 260.00),
    ('2026-02-04', 260.00),
    ('2026-02-05', 410.00);
