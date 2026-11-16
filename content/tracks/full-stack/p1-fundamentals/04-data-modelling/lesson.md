# From Screens to Tables: Data Modelling, Migrations and Page Queries

The SQL track teaches the language and the relational model. This lesson is about the full
stack workflow around them: going from a screen to a schema, changing that schema safely, and
loading exactly what a page needs.

## Start from the screens and the rules

List what each screen shows and what must always be true, then let the nouns become tables:

> *"My orders" shows a customer's orders, newest first, each with its lines and total. An order
> belongs to one customer. A line has a product, a quantity (at least 1) and the price paid.*

```sql
CREATE TABLE orders (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES customers (id),
    placed_at   timestamptz NOT NULL DEFAULT now(),
    status      text NOT NULL DEFAULT 'placed'
                CHECK (status IN ('placed', 'shipped', 'delivered', 'cancelled'))
);

CREATE TABLE order_items (
    order_id         bigint NOT NULL REFERENCES orders (id),
    product_id       bigint NOT NULL REFERENCES products (id),
    quantity         integer NOT NULL CHECK (quantity > 0),
    unit_price_cents integer NOT NULL CHECK (unit_price_cents >= 0),
    PRIMARY KEY (order_id, product_id)
);
```

- **Constraints are part of the feature.** `NOT NULL`, `CHECK`, `UNIQUE` and foreign keys refuse
  bad data even when a bug or a second service skips your validation.
- **Copy what must not change.** The price paid is stored on the line; a later price change
  must not rewrite old orders.
- **Money in integer cents**, timestamps as `timestamptz`.
- **Many-to-many needs a join table** (`task_labels (task_id, label_id)` with a primary key on the
  pair).

## Migrations

The schema lives in **versioned migration files** in the repository (Flyway, Liquibase, Prisma
Migrate, Alembic, EF Core migrations, golang-migrate), run in order and recorded in a table of the
database. Rules of thumb:

- every schema change is a reviewed migration, never a manual change in production;
- migrations are **forward-only** in practice: fix a bad one with a new one;
- run them **before** the new code starts (a separate step or a one-shot job, as you will do in
  the Compose kata), not concurrently from every instance;
- keep them **backwards compatible** with the code still running: add a nullable or defaulted
  column now, use it in the next release, drop old ones later (expand and contract again).

## ORM or SQL?

An ORM (JPA/Hibernate, Entity Framework, Prisma, SQLAlchemy) maps rows to objects and writes
simple queries for you. It is productive for CRUD and dangerous when you stop looking at the SQL
it runs. Query builders and plain SQL give full control for reports and complex pages. Most
teams use both: the ORM for writes and simple reads, SQL for the queries that matter. Whatever
you use, turn on SQL logging in development and read it.

## The N+1 problem

```ts
const orders = await db.query("SELECT * FROM orders WHERE customer_id = $1", [id]);  // 1 query
for (const order of orders) {
  order.items = await db.query("SELECT * FROM order_items WHERE order_id = $1", [order.id]);  // N queries
}
```

Fast with three orders on your laptop, slow with fifty in production. Fixes: a **join**, a second
query with `WHERE order_id = ANY($1)` for all ids at once, the ORM's fetch join or batch size, or
let PostgreSQL build the JSON:

```sql
SELECT o.id, jsonb_agg(jsonb_build_object('product', p.name, 'quantity', i.quantity)
                       ORDER BY p.name) AS items
FROM orders o
JOIN order_items i ON i.order_id = o.id
JOIN products p ON p.id = i.product_id
WHERE o.customer_id = $1
GROUP BY o.id;
```

## Pages: keyset, not offset

`LIMIT 20 OFFSET 4000` reads and throws away 4,000 rows, and shows a duplicate or skips a row
when new data arrives between page loads. **Keyset pagination** continues from the last row seen:

```sql
SELECT id, placed_at FROM orders
WHERE customer_id = $1
  AND (placed_at, id) < ($2, $3)          -- the cursor: the last row of the previous page
ORDER BY placed_at DESC, id DESC          -- id breaks ties between equal timestamps
LIMIT 21;                                 -- one more than the page: is there a next one?
```

with the index that matches filter and order: `CREATE INDEX ON orders (customer_id, placed_at
DESC, id DESC)`. The API hands the client an **opaque cursor** (for example base64 of the last
`placed_at` and `id`); the client sends it back and never builds one itself. Offsets are fine for
small admin tables where users jump to page 7.

## Summary

- Derive tables from screens and rules; constraints are part of the feature.
- Change schemas with versioned, forward-only, backwards-compatible migrations run before deploys.
- Read the SQL your ORM runs; fix N+1 with joins, batching or JSON aggregation.
- Paginate with keysets, a tie-breaker, a matching index and opaque cursors.

## Further reading

- PostgreSQL documentation, "Constraints": <https://www.postgresql.org/docs/current/ddl-constraints.html>
- PostgreSQL documentation, "JSON Functions and Operators": <https://www.postgresql.org/docs/current/functions-json.html>
- Markus Winand, "We need tool support for keyset pagination" (Use The Index, Luke): <https://use-the-index-luke.com/no-offset>
- Flyway documentation, "Migrations": <https://documentation.red-gate.com/fd/migrations-271585107.html>
- Martin Fowler and Pramod Sadalage, "Evolutionary Database Design": <https://martinfowler.com/articles/evodb.html>
