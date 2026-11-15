SELECT dojo.query_eq(
    'a tie at the top shares rank 1 and the next salary is rank 2',
    'salary_rank.sql',
    $$VALUES ('Support', 'Joana Faria', 3900.00, 1), ('Support', 'Luis Neves', 3900.00, 1),
             ('Support', 'Nuno Alves', 2500.00, 2)$$,
    ordered => true,
    setup => $$UPDATE employees SET manager_id = NULL;
                DELETE FROM employees WHERE department_id <> 3;
                UPDATE employees SET salary = 3900.00 WHERE id = 10;
                INSERT INTO employees VALUES (12, 'Nuno Alves', 3, 9, 2500.00)$$
);

SELECT dojo.query_eq(
    'keeps the two best paid of each department',
    'top_earners.sql',
    $$VALUES ('Engineering', 'Ines Moura', 9800.00), ('Engineering', 'Bruno Reis', 7200.00),
             ('Sales', 'Carla Dias', 7200.00), ('Sales', 'Gabriela Pinto', 5200.00),
             ('Support', 'Joana Faria', 3900.00), ('Support', 'Luis Neves', 3100.00)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'keeps everyone tied at the second place',
    'top_earners.sql',
    $$VALUES ('Engineering', 'Ines Moura', 9800.00), ('Engineering', 'Bruno Reis', 7200.00),
             ('Engineering', 'Duarte Lima', 7200.00),
             ('Sales', 'Carla Dias', 7200.00), ('Sales', 'Gabriela Pinto', 5200.00),
             ('Support', 'Joana Faria', 3900.00), ('Support', 'Luis Neves', 3100.00)$$,
    ordered => true,
    setup => $$UPDATE employees SET salary = 7200.00 WHERE id = 4$$
);

SELECT dojo.query_eq(
    'a tie at the top leaves no room for a third',
    'top_earners.sql',
    $$VALUES ('Sales', 'Carla Dias', 7200.00), ('Sales', 'Hugo Matos', 7200.00)$$,
    ordered => true,
    setup => $$UPDATE employees SET manager_id = NULL;
                DELETE FROM employees WHERE department_id <> 2;
                UPDATE employees SET salary = 7200.00 WHERE id = 8$$
);

SELECT dojo.query_eq(
    'computes month to date, changes and moving averages',
    'running_sales.sql',
    $$VALUES (DATE '2026-01-28', 300.00, 300.00, NULL::numeric, 300.00),
             (DATE '2026-01-29', 420.00, 720.00, 120.00, 360.00),
             (DATE '2026-01-31', 380.00, 1100.00, -40.00, 366.67),
             (DATE '2026-02-02', 150.00, 150.00, -230.00, 316.67),
             (DATE '2026-02-03', 260.00, 410.00, 110.00, 263.33),
             (DATE '2026-02-04', 260.00, 670.00, 0.00, 223.33),
             (DATE '2026-02-05', 410.00, 1080.00, 150.00, 310.00)$$,
    ordered => true
);

SELECT dojo.query_eq(
    'a new month starts the total again',
    'running_sales.sql',
    $$VALUES (DATE '2026-02-27', 100.00, 100.00, NULL::numeric, 100.00),
             (DATE '2026-02-28', 50.00, 150.00, -50.00, 75.00),
             (DATE '2026-03-01', 90.00, 90.00, 40.00, 80.00),
             (DATE '2026-03-02', 10.00, 100.00, -80.00, 50.00)$$,
    ordered => true,
    setup => $$DELETE FROM daily_sales;
                INSERT INTO daily_sales VALUES (DATE '2026-03-02', 10.00), (DATE '2026-02-27', 100.00),
                                               (DATE '2026-03-01', 90.00), (DATE '2026-02-28', 50.00)$$
);

SELECT dojo.query_eq(
    'walks the reporting line below Bruno',
    'reports_to.sql',
    $$VALUES ('Bruno Reis', 0, 'Bruno Reis'),
             ('Duarte Lima', 1, 'Bruno Reis > Duarte Lima'),
             ('Eva Rocha', 2, 'Bruno Reis > Duarte Lima > Eva Rocha'),
             ('Filipe Sousa', 1, 'Bruno Reis > Filipe Sousa'),
             ('Marta Gomes', 2, 'Bruno Reis > Filipe Sousa > Marta Gomes')$$,
    ordered => true
);

SELECT dojo.query_eq(
    'finds a new hire three levels down',
    'reports_to.sql',
    $$VALUES ('Bruno Reis', 0, 'Bruno Reis'),
             ('Duarte Lima', 1, 'Bruno Reis > Duarte Lima'),
             ('Eva Rocha', 2, 'Bruno Reis > Duarte Lima > Eva Rocha'),
             ('Olga Ramos', 3, 'Bruno Reis > Duarte Lima > Eva Rocha > Olga Ramos'),
             ('Filipe Sousa', 1, 'Bruno Reis > Filipe Sousa'),
             ('Marta Gomes', 2, 'Bruno Reis > Filipe Sousa > Marta Gomes')$$,
    ordered => true,
    setup => $$INSERT INTO employees VALUES (12, 'Olga Ramos', 1, 5, 3000.00)$$
);
