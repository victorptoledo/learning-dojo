# Analytics

The company's tables are in `fixture/01_company.sql`:

```text
departments (id, name)
employees   (id, name, department_id → departments, manager_id → employees, salary)
daily_sales (day, revenue)      -- one row per day the shop was open; closed days have no row
```

Write one query in each file:

| File | Columns | Rows and order |
| --- | --- | --- |
| `salary_rank.sql` | department name, employee name, salary, rank | every employee, ranked by salary inside their department, highest first; equal salaries share a rank and the next rank follows without a gap (1, 2, 2, 3); order by department, rank, employee |
| `top_earners.sql` | department name, employee name, salary | per department, the employees whose salary rank is 1 or 2, where ties share a rank and leave a gap (1, 1, 3): two people tied at the top leave no second place; order by department, salary from highest, employee |
| `running_sales.sql` | day, revenue, month-to-date total, change, moving average | every row of `daily_sales`; the total of the month up to this day; revenue minus the previous **row**'s (NULL for the first row); the average of this row and the 2 rows before it (fewer at the start), rounded to 2 decimals; oldest first |
| `reports_to.sql` | name, depth, path | Bruno Reis (depth 0) and everyone who reports to him directly or indirectly; the path joins the names from Bruno with `' > '`; order by path |

Try your queries with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`.

There are 9 tests: 1 in `tests/visible.sql` and 8 hidden ones (ties, a new month, a new
hire further down).
