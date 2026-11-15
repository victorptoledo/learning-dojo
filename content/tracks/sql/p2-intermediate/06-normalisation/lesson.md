# Normalisation and Schema Design

Queries are only as good as the schema under them. Normalisation is the discipline of
storing **each fact once**, so that a change happens in one place and contradictions cannot
exist. You do not need the theory to its last letter, but you need its reflexes.

## The problem: redundancy and anomalies

```text
order_lines
order_id | customer_email   | customer_name | product_code | product_name | price | qty
101      | ada@example.com  | Ada Lovelace  | SW-01        | Bamboo Sword | 24.90 | 1
101      | ada@example.com  | Ada Lovelace  | BL-02        | Cotton Belt  |  9.50 | 2
102      | ada@example.com  | Ada L.        | SW-01        | Bamboo Swrd  | 24.90 | 1
```

| Anomaly | Here |
| --- | --- |
| **Update** | Ada changes her name: every row must change, and the third row already disagrees |
| **Insert** | a new product cannot be stored until someone orders it |
| **Delete** | delete order 102 and the only record of a customer may vanish |

All three come from the same cause: facts about different **things** (customers, products,
orders) are stored in one table.

## Functional dependencies

`A → B` ("A determines B") means: rows with the same A always have the same B. Here:

- `customer_email → customer_name`
- `product_code → product_name, price`
- `(order_id, product_code) → qty`
- `order_id → customer_email`

A good table has dependencies only from its **key**. Every arrow that starts from something
that is not a key is a fact that wants its own table.

## The normal forms in plain words

| Form | Rule | Breaks it |
| --- | --- | --- |
| **1NF** | every cell holds one value; no repeating groups | `phones = '911..., 922...'`, columns `phone1, phone2, phone3` |
| **2NF** | 1NF, and no column depends on **part** of a composite key | `product_name` depends only on `product_code` in a table keyed by `(order_id, product_code)` |
| **3NF** | 2NF, and no column depends on another **non-key** column | `customer_name` depends on `customer_email`, which depends on `order_id` |
| **BCNF** | every determinant is a candidate key | rare cases with overlapping candidate keys |

The everyday summary (attributed to Bill Kent): every non-key column depends on "the key, the
whole key and nothing but the key". 4NF and 5NF deal with independent multi-valued facts
(a teacher's subjects and languages in one table); in practice, giving each many-to-many
relationship its own table avoids them.

The normalised shop:

```sql
CREATE TABLE customers (id bigint PRIMARY KEY, email text NOT NULL UNIQUE, name text NOT NULL);
CREATE TABLE products  (code text PRIMARY KEY, name text NOT NULL, price numeric(8, 2) NOT NULL);
CREATE TABLE orders    (id bigint PRIMARY KEY, customer_id bigint NOT NULL REFERENCES customers (id),
                        placed_at timestamptz NOT NULL);
CREATE TABLE order_lines (
    order_id     bigint NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_code text   NOT NULL REFERENCES products (code),
    quantity     integer NOT NULL CHECK (quantity > 0),
    unit_price   numeric(8, 2) NOT NULL,          -- see "denormalise on purpose"
    PRIMARY KEY (order_id, product_code)
);
```

## Modelling relationships

| Relationship | Shape | Example |
| --- | --- | --- |
| one-to-many | a foreign key on the "many" side | `orders.customer_id` |
| many-to-many | a **junction table** with two foreign keys, keyed by the pair | `enrolments (student_id, course_id)` |
| one-to-one | a foreign key that is also `UNIQUE` (or shares the primary key) | `users` and `user_settings` |
| hierarchy | a foreign key to the same table | `employees.manager_id`, `categories.parent_id` |

A junction table often grows attributes of its own: an enrolment has a date and a grade; a
membership has a role. That is a sign it is a real entity.

Patterns to avoid: a comma-separated list of ids in one column (no foreign key, no index, no
join), and the **entity-attribute-value** table `(entity_id, attribute, value text)` that
stores any field as text (no types, no constraints, painful queries). If attributes are truly
open-ended, a `jsonb` column next to the real columns is the lesser evil (Phase 3).

## Denormalise on purpose

Normalisation is the default, not a religion. Copies are fine when you **decide** them and
know how they stay correct:

- **Snapshots of history.** `order_lines.unit_price` copies the price at the time of the order.
  It is not redundant: it is a different fact ("what this customer paid"), and it must not
  change when the catalogue does.
- **Derived values for speed.** A `comments_count` on posts, a materialized view for a
  dashboard, a summary table filled by a job. Keep them correct with a trigger, the same
  transaction that changes the source, or a scheduled rebuild, and accept their lag.
- **Read models and analytics.** Warehouses use star schemas (a wide fact table and dimension
  tables) because they are read far more than written.

Measure first: joins on indexed keys are cheap, and most "the joins are slow" problems are
missing indexes.

## Summary

- Redundancy causes update, insert and delete anomalies.
- A functional dependency A → B says A determines B; non-key determinants want their own table.
- 1NF: one value per cell; 2NF: no partial dependency; 3NF: no transitive dependency.
- Many-to-many needs a junction table; avoid id lists in a column and EAV tables.
- Denormalise deliberately (price at purchase time, counters, read models) and keep copies
  correct.

## Further reading

- Database normalization (with the normal forms and examples): <https://en.wikipedia.org/wiki/Database_normalization>
- PostgreSQL 17, "Data Definition" (tables, constraints, schemas): <https://www.postgresql.org/docs/17/ddl.html>
- PostgreSQL wiki, "Don't Do This": <https://wiki.postgresql.org/wiki/Don%27t_Do_This>
- *SQL Antipatterns* by Bill Karwin (jaywalking, EAV): <https://pragprog.com/titles/bksap1/sql-antipatterns-volume-1/>
- roadmap.sh SQL topics (RDBMS benefits, data integrity): <https://github.com/nilbuild/developer-roadmap/tree/master/roadmaps/sql/content>
