# 🥋 Checkpoint 4: The Billing Module

You inherit the billing module of a small SaaS. `fixture/01_billing.sql` creates it:

```text
plans     (id, name, monthly_fee double precision)
customers (id, name, email, plan_ids text)        plan_ids: '2,4', typed by hand
usage     (id, customer_id → customers, recorded_at timestamptz, units)
invoices  (id, customer_id → customers, period date, amount numeric(10, 2))
          period is the first day of the month; UNIQUE (customer_id, period)
search_customers(term text) → TABLE (id, name)     builds its SQL by pasting term in
```

Fix it in four files. Each one is graded on its own: the tests of `close_month.sql` start from a
database that is already migrated, whatever your `migrate.sql` does.

**1. `migrate.sql`** — the schema smells. In one script:

- `plans.monthly_fee` becomes `numeric(10, 2)`, keeping every fee (9.9 becomes 9.90).
- A new table `subscriptions (customer_id, plan_id)` holds one row per plan a customer pays for,
  with a foreign key to `customers` and one to `plans`, both required, and no pair twice.
- Fill it from `plan_ids`: ignore spaces and empty pieces (`' 3, 5 ,4'`, `'2,'`), keep a plan
  listed twice once (`'1,1'`), and drop ids of plans that no longer exist (plan 6).
- Then drop the column `customers.plan_ids`.

**2. `close_month.sql`** — close **November 2026**. Every customer with at least one
subscription or some usage in the month gets an invoice for `period = 2026-11-01` whose amount is
the sum of the monthly fees of their plans plus **0.02 per unit** of usage, rounded to cents.
The month runs from midnight to midnight in **São Paulo time** (`America/Sao_Paulo`), although
`recorded_at` is stored in UTC. Write it set-based (no loop over customers), and safe to run again:
a second run must not fail or duplicate anything, and a run after late usage arrived updates the
November invoices to the new amounts. Invoices of other months stay as they are.

**3. `search_customers.sql`** — rewrite the function with `CREATE OR REPLACE FUNCTION` and the
same signature. It returns the customers whose name **contains** the term, ignoring case, by id.
The term is data, never SQL: a quote, a `%` or a `_` in it is just that character
(`search_customers('100%')` finds only the coffee shop, and a term like
`' OR true OR name ILIKE '` finds nobody).

**4. `latest_invoices.sql`** — the account page shows a page of customers, each with their three
latest invoices, and the application runs one query per customer:

```text
for id in page_ids:                                   -- 1 + 50 queries per page
    db.query("SELECT period, amount FROM invoices
              WHERE customer_id = $1 ORDER BY period DESC LIMIT 3", id)
```

Write a function `latest_invoices(ids integer[])` that returns `TABLE (customer_id integer,
period date, amount numeric)` with **one query** for the whole page: for every existing customer
whose id is in `ids` (each once, whatever the order or repeats in the array), their three latest
invoices, newest first; a customer without invoices appears once, with NULL period and amount.
Rows are ordered by customer id, then period from newest to oldest.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`: call
`SELECT * FROM search_customers('100%')` before and after `\i search_customers.sql`, and run
`\i migrate.sql` inside `BEGIN` ... `ROLLBACK` until the subscriptions come out right.

There are 22 tests: 1 in `tests/visible.sql` and 21 hidden ones.
