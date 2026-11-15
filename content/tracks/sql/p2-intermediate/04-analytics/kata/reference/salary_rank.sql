SELECT d.name AS department,
       e.name AS employee,
       e.salary,
       dense_rank() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) AS salary_rank
FROM employees AS e
JOIN departments AS d ON d.id = e.department_id
ORDER BY department, salary_rank, employee;
