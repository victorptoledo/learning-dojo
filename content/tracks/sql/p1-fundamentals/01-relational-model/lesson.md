# The Relational Model: Tables, Keys and Data Types

In 1970 Edgar F. Codd proposed storing data as **relations**: sets of facts with the same
shape, queried by a small algebra instead of by following pointers. Every SQL engine is
built on that idea, so a little theory makes the rest of the track easier.

## Tables, rows and columns

| Model term | SQL term | Means |
| --- | --- | --- |
| relation | table | a set of facts of one kind |
| tuple | row | one fact: "customer 7 is Ada, from Portugal" |
| attribute | column | a named, typed property of every row |
| domain | data type | the values a column may hold |

Two properties matter every day:

- **Rows have no order.** A table is a set (strictly, SQL allows duplicates, so a bag). The
  engine returns rows in whatever order is cheapest. Only `ORDER BY` guarantees an order.
- **Each cell holds one value** of the column's type. A list of phone numbers in one text
  cell breaks the model; it belongs in its own table (Phase 2, normalisation).

## Keys

A **primary key** identifies each row: unique and never NULL. A **foreign key** is a column
whose values must exist as a key in another table. That is how tables refer to each other:

```sql
CREATE TABLE customers (
    id      integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email   text NOT NULL UNIQUE,
    name    text NOT NULL,
    country char(2) NOT NULL
);

CREATE TABLE orders (
    id          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id integer NOT NULL REFERENCES customers (id),
    placed_at   timestamptz NOT NULL DEFAULT now(),
    status      text NOT NULL DEFAULT 'new'
);
```

`orders.customer_id` may only hold ids that exist in `customers`. The engine checks it on
every insert and update, so no order can point to a customer that does not exist.

- A **natural key** comes from the data (an email, an ISBN). A **surrogate key** is an
  invented number or UUID. Surrogate keys never change when the data does; keep the natural
  key as `UNIQUE` too.
- `GENERATED ALWAYS AS IDENTITY` is the standard way to number rows. You will also see
  PostgreSQL's older `serial`, MySQL's `AUTO_INCREMENT`, SQL Server's `IDENTITY(1,1)` and
  SQLite's `INTEGER PRIMARY KEY`.

## Data types that matter

| Kind | PostgreSQL | Use for | Watch out |
| --- | --- | --- | --- |
| Whole numbers | `integer`, `bigint`, `smallint` | ids, counts | `integer` ends at about 2.1 billion; use `bigint` for busy ids |
| Exact decimals | `numeric(p, s)` | **money**, quantities | slower than floats, but exact |
| Floating point | `real`, `double precision` | measurements, science | `0.1 + 0.2` is not `0.3`: never money |
| Text | `text`, `varchar(n)`, `char(n)` | names, codes | in PostgreSQL `text` and `varchar` perform the same |
| Boolean | `boolean` | flags | MySQL uses `tinyint(1)`, SQL Server `bit` |
| Date | `date` | birthdays, due dates | no time, no zone |
| Point in time | `timestamptz` | events, created_at | stored as UTC, shown in the session's zone |
| Local time | `timestamp` | a wall-clock time with no zone | ambiguous across zones |
| Interval | `interval` | durations: `interval '3 days'` | |
| Identifiers | `uuid` | ids made outside the database | |
| Documents | `jsonb` | flexible attributes (Phase 3) | not a reason to skip a schema |

> [!TIP]
> Store points in time as `timestamptz`. The name is misleading: it does not store a zone,
> it stores an instant (in UTC) and converts on input and output. `timestamp` without zone
> is a wall-clock reading, and two of them from different zones cannot be compared.

## Changing the schema

```sql
ALTER TABLE customers ADD COLUMN phone text;            -- new column, NULL in old rows
ALTER TABLE customers ALTER COLUMN name TYPE varchar(200);
ALTER TABLE customers RENAME COLUMN phone TO mobile;
ALTER TABLE customers DROP COLUMN mobile;
DROP TABLE orders;                                      -- the data is gone
DROP TABLE IF EXISTS orders;                            -- no error if missing
TRUNCATE orders;                                        -- empty it, keep the table
```

In PostgreSQL, DDL is transactional: inside `BEGIN ... ROLLBACK` a `DROP TABLE` can be
undone. MySQL and Oracle commit DDL immediately.

## The algebra under every query

Codd's algebra has a handful of operations. Each one takes tables and returns a table, so
they compose:

| Operation | Does | SQL |
| --- | --- | --- |
| selection (σ) | keep rows that match | `WHERE` |
| projection (π) | keep some columns | the `SELECT` list |
| product (×) | every row with every row | `CROSS JOIN` |
| join (⋈) | product plus a condition | `JOIN ... ON` |
| union, difference, intersection | set operations | `UNION`, `EXCEPT`, `INTERSECT` |
| rename (ρ) | new names | `AS` |

Because every step returns a table, you can put a query where a table is expected: that is
what subqueries, CTEs and views are (Phase 2).

## Summary

- A table is a set of rows of one shape; rows have no order without `ORDER BY`.
- A primary key identifies rows; a foreign key must match a key in another table.
- Use `numeric` for money, `timestamptz` for instants, `bigint` for ids that grow.
- `CREATE`, `ALTER`, `DROP`, `TRUNCATE` change the schema; PostgreSQL can roll DDL back.
- Queries are compositions of selection, projection, join and set operations.

## Further reading

- PostgreSQL 17, "Data Definition": <https://www.postgresql.org/docs/17/ddl.html>
- PostgreSQL 17, "Data Types": <https://www.postgresql.org/docs/17/datatype.html>
- PostgreSQL 17, "Date/Time Types": <https://www.postgresql.org/docs/17/datatype-datetime.html>
- The relational model: <https://en.wikipedia.org/wiki/Relational_model>
- roadmap.sh SQL topics (data types, DDL, keys): <https://github.com/nilbuild/developer-roadmap/tree/master/roadmaps/sql/content>
