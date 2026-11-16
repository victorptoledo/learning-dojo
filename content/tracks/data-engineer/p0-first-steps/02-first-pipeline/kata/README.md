# Your First Pipeline: CSV to SQLite

Every pipeline does the same three things: **extract** data from a source, **transform** it
into something trustworthy, and **load** it where people query it. Here the source is
`data/orders.csv`, an export from a shop, and the destination is a SQLite database
(`sqlite3` from the standard library). Write the four functions of `pipeline.py`:

| Function | Returns |
| --- | --- |
| `extract(path)` | every data row of the CSV as a `dict` of column name -> raw text |
| `transform(rows)` | `(orders, rejected)`: the clean `Order`s, and `(line, reason)` for each row that was refused |
| `load(conn, orders)` | the number of rows written to the `orders` table |
| `run(csv_path, db_path)` | `{"read": n, "loaded": n, "rejected": n}` for the whole file |

## Extract

Read the file as UTF-8 and tolerate a byte order mark (Excel writes one). Use the `csv`
module: a quoted field such as `"Lovelace, Ada"` contains a comma.

## Transform

Check the fields in this order; the **first** bad one is the reason the row is rejected:

| Field | Clean value | Reason when bad |
| --- | --- | --- |
| `order_id` | an `int` greater than 0 (spaces around it are fine) | `bad order_id` |
| `customer` | the text without surrounding spaces, not empty | `missing customer` |
| `amount` | a number, 0 or more, with at most 2 decimals, as **integer cents** (`"5"` is 500) | `bad amount` |
| `country` | two letters, upper-cased (`gb` becomes `GB`) | `bad country` |
| `order_date` | an ISO date `YYYY-MM-DD` that exists, as a `datetime.date` | `bad order_date` |

An `order_id` that already appeared in an **accepted** row is rejected as
`duplicate order_id` (the first accepted one wins). The line numbers are those of the CSV
file: the header is line 1, so the first data row is line 2.

> [!TIP]
> Parse money with `decimal.Decimal`, never `float`: `0.1 + 0.2` is not `0.3` in binary
> floating point, and an amount like `12.345` must be refused, not rounded.

## Load

Create the table if it does not exist, with exactly these columns:

```sql
orders (order_id INTEGER PRIMARY KEY, customer TEXT, amount_cents INTEGER,
        country TEXT, order_date TEXT)   -- order_date as 'YYYY-MM-DD'
```

Write all orders in **one transaction** and commit it. The load must be **idempotent**:
loading the same orders twice leaves the same table, and loading an order again with new
values updates its row (an upsert, `INSERT ... ON CONFLICT (order_id) DO UPDATE`).

## Run

Wire the three steps together and close the connection. Running it twice on the same file
must give the same report and the same table.

Run the visible tests with `uv run pytest`. There are 28 tests: 2 in `tests/test_visible.py`
and 25 hidden ones, plus ruff on `pipeline.py`.
