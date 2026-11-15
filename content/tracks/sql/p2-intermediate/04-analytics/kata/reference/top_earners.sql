WITH ranked AS (
    SELECT d.name AS department,
           e.name AS employee,
           e.salary,
           rank() OVER (PARTITION BY e.department_id ORDER BY e.salary DESC) AS salary_rank
    FROM employees AS e
    JOIN departments AS d ON d.id = e.department_id
)
SELECT department, employee, salary
FROM ranked
WHERE salary_rank <= 2
ORDER BY department, salary DESC, employee;
