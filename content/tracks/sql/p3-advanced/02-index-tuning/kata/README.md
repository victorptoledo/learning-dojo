# Index Tuning

The tables are in `fixture/01_orders.sql`: 50 000 `customers (id, email, country,
created_at)` and 200 000 `orders (id, customer_id, status, placed_at, total)`, generated the
same way every time. Besides the primary keys there is one index, `orders_placed_at` on
`orders (placed_at)`.

## Part 1: `indexes.sql`

Three pages run these queries:

```sql
-- the 10 latest orders of a customer
SELECT id, placed_at, total FROM orders WHERE customer_id = 42 ORDER BY placed_at DESC LIMIT 10;

-- login: emails are compared without case
SELECT id FROM customers WHERE lower(email) = 'user4242@example.com';

-- the 50 oldest pending orders, for the warehouse
SELECT id, placed_at FROM orders WHERE status = 'pending' ORDER BY placed_at LIMIT 50;
```

Write **at most three** `CREATE INDEX` statements so that, after `ANALYZE`, the plan of each
query has no `Seq Scan`, no `Sort` and no `Filter` line: the index finds exactly the rows, in
the right order. Change no data and do not touch the planner's settings.

## Part 2: `march_orders.sql`

The monthly report runs:

```sql
SELECT count(*) AS orders, sum(total) AS revenue
FROM orders
WHERE date_trunc('month', placed_at) = TIMESTAMPTZ '2025-03-01 00:00+00'
  AND status = 'paid';
```

It reads all 200 000 orders even though `orders_placed_at` exists. Rewrite it so that it returns
**exactly the same** numbers and its plan can use `orders_placed_at` (without the indexes of
part 1). Times are in UTC.

## Try it

```bash
docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play
```

```text
dojo=# EXPLAIN SELECT id FROM customers WHERE lower(email) = 'user4242@example.com';
dojo=# BEGIN;
dojo=# \i indexes.sql
dojo=# ANALYZE;
dojo=# EXPLAIN SELECT id FROM customers WHERE lower(email) = 'user4242@example.com';
dojo=# ROLLBACK;
```

There are 8 tests: 1 in `tests/visible.sql` and 7 hidden ones.
