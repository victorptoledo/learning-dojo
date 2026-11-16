# Checkpoint 1: A Sales Warehouse from Files

A shop sends two files every night: `data/customers.csv` and `data/orders.jsonl` (one JSON
object per line). Build the warehouse that turns them into a small star schema on SQLite,
with loads you can rerun safely. Write the five functions of `warehouse.py`.

## `create_schema(conn)`

Create these tables (with these columns, in this order). Running it twice must not fail.

```text
dim_customer (customer_key INTEGER PRIMARY KEY, customer_id INTEGER UNIQUE, name, country)
dim_date     (date_key INTEGER PRIMARY KEY, full_date TEXT UNIQUE, year, month)
fact_sales   (order_id, line_no, date_key, customer_key, sku, quantity, revenue_cents)
```

`customer_key` is the surrogate key (SQLite assigns it), `customer_id` the natural key.
`date_key` is the day as the number `YYYYMMDD` and `full_date` is `'YYYY-MM-DD'`. The
fact table's grain is one row per order line: `(order_id, line_no)` is its primary key, and
`date_key` and `customer_key` reference the dimensions.

## `load_customers(conn, path)` -> rows read

Upsert every row of the CSV on `customer_id`. A known customer keeps its `customer_key` and
gets the new name and country (a type 1 change: history is overwritten).

## `load_orders(conn, path)` -> `LoadReport(loaded, rejected)`

Each non-blank line is an order:

```json
{"order_id": 1, "customer_id": 1, "ordered_at": "2026-11-01T10:00:00+00:00",
 "lines": [{"sku": "KB-1", "quantity": 1, "unit_price": 80.00}]}
```

Check each record in this order; the first problem is the reason it is rejected, with its
line number in the file (the first line is 1):

| Problem | Reason |
| --- | --- |
| the line is not valid JSON | `invalid json` |
| it is not an object, or lacks `order_id`, `customer_id`, `ordered_at` or `lines` | `missing field` |
| `order_id` is not an integer greater than 0 (`true` is not an integer) | `bad order_id` |
| `ordered_at` is not an ISO timestamp **with** a UTC offset | `bad timestamp` |
| `lines` is empty, or a line lacks a non-blank `sku`, an integer `quantity` > 0 or a `unit_price` that is a JSON number, 0 or more, with at most 2 decimals | `bad line` |
| `customer_id` is not the id of a customer in `dim_customer` | `unknown customer` |

For each accepted record:

- the day is the **UTC** date of `ordered_at` (`23:30-03:00` on the 1st is the 2nd in UTC);
  add it to `dim_date` if it is not there yet;
- `line_no` is the position in `lines` (1, 2...), `revenue_cents` is
  `quantity * unit_price` in integer cents;
- the record **replaces** whatever the fact table holds for that `order_id`, so a corrected
  order sent again (even later in the same file) leaves only its new lines.

Commit the load. `loaded` counts the accepted records. Loading the same file twice gives the
same tables.

> [!TIP]
> `json.loads(text, parse_float=Decimal)` reads `19.99` as `Decimal("19.99")` instead of a
> binary float, so the cents stay exact.

## The reports

| Function | Returns |
| --- | --- |
| `revenue_by_country(conn)` | `[(country, revenue_cents)]`, the largest revenue first, ties by country |
| `revenue_by_month(conn)` | `[(year, month, revenue_cents)]` in calendar order |

Run the visible tests with `uv run pytest`. There are 28 tests: 1 in `tests/test_visible.py`
and 26 hidden ones, plus ruff on `warehouse.py`.
