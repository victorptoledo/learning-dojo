-- Each test runs one of your files and compares its rows with the expected ones.
SELECT dojo.query_eq(
    'ranks salaries inside each department',
    'salary_rank.sql',
    $$VALUES ('Engineering', 'Ines Moura', 9800.00, 1), ('Engineering', 'Bruno Reis', 7200.00, 2),
             ('Engineering', 'Duarte Lima', 6100.00, 3), ('Engineering', 'Filipe Sousa', 6100.00, 3),
             ('Engineering', 'Eva Rocha', 4800.00, 4), ('Engineering', 'Marta Gomes', 4800.00, 4),
             ('Sales', 'Carla Dias', 7200.00, 1), ('Sales', 'Gabriela Pinto', 5200.00, 2),
             ('Sales', 'Hugo Matos', 4300.00, 3),
             ('Support', 'Joana Faria', 3900.00, 1), ('Support', 'Luis Neves', 3100.00, 2)$$,
    ordered => true
);
