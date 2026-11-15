# Query Clinic

The tables are in `fixture/01_clinic.sql`:

```text
customers (id, name, referred_by → customers; NULL when nobody referred them)
orders    (id, customer_id → customers, placed_at timestamptz, total)   -- 3000 orders, 2026 Jan to Apr
tickets   (id, customer_id → customers, opened_at)
indexes: orders (placed_at, id), orders (customer_id), tickets (customer_id)
```

Each file has a **patient**: a query from production with a complaint. Write the cure in the
file. Keep the columns of the patient, in the same order.

**1. `no_referrals.sql`** — "the referral report is empty". It should list the names of the
customers who referred nobody, alphabetically.

```sql
SELECT name FROM customers WHERE id NOT IN (SELECT referred_by FROM customers) ORDER BY name;
```

**2. `customer_totals.sql`** — "the totals are too high, and some customers are missing". It
should list every customer with the total of their orders and their number of tickets (0 when
none), by name.

```sql
SELECT c.name, sum(o.total) AS revenue, count(t.id) AS tickets
FROM customers c
JOIN orders o ON o.customer_id = c.id
JOIN tickets t ON t.customer_id = c.id
GROUP BY c.name
ORDER BY c.name;
```

**3. `next_page.sql`** — "page 76 is slow and sometimes repeats an order". The client has just
shown the page whose last order was **id 1500, placed at 2026-02-18 23:00 UTC**; return the next
20 orders in `(placed_at, id)` order, so that the index `orders_placed_id` can seek straight to
that position.

```sql
SELECT id, placed_at, total FROM orders ORDER BY placed_at, id LIMIT 20 OFFSET 1500;
```

**4. `march_revenue.sql`** — "March is missing a day of sales". It should count and sum every
order placed in March 2026, in UTC.

```sql
SELECT count(*) AS orders, sum(total) AS revenue
FROM orders
WHERE placed_at BETWEEN '2026-03-01' AND '2026-03-31';
```

Try the patients and your cures with
`docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`, and `EXPLAIN` the page
query.

There are 9 tests: 1 in `tests/visible.sql` and 8 hidden ones; one of them reads the plan of
`next_page.sql`.
