# Constraints and Keys

Every rule the application checks can be broken by another application, a migration script,
a race between two requests or a hurried fix in psql. A **constraint** is a rule the database
checks on every write, from every client, inside the transaction. It is the cheapest bug
prevention you will ever buy.

## The constraints

```sql
CREATE TABLE accounts (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      text NOT NULL UNIQUE,
    plan       text NOT NULL DEFAULT 'free' CHECK (plan IN ('free', 'pro', 'team')),
    seats      integer NOT NULL CHECK (seats > 0),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE invoices (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id bigint NOT NULL REFERENCES accounts (id) ON DELETE RESTRICT,
    period     date NOT NULL,
    due_on     date NOT NULL,
    amount     numeric(12, 2) NOT NULL CHECK (amount >= 0),
    paid_at    timestamptz,
    CONSTRAINT one_invoice_per_period UNIQUE (account_id, period),
    CONSTRAINT due_after_period CHECK (due_on >= period)
);
```

| Constraint | Guarantees | Violation (SQLSTATE) |
| --- | --- | --- |
| `NOT NULL` | a value is present | `23502` not_null_violation |
| `CHECK (expr)` | the expression is not FALSE for the row | `23514` check_violation |
| `UNIQUE (cols)` | no two rows share the values (NULLs excepted) | `23505` unique_violation |
| `PRIMARY KEY (cols)` | UNIQUE + NOT NULL, one per table | `23505` / `23502` |
| `REFERENCES t (cols)` | the values exist as a key in `t` | `23503` foreign_key_violation |
| `EXCLUDE USING gist (...)` | no two rows "conflict" by an operator | `23P01` exclusion_violation |

All integrity violations share the class `23`. Applications should map them to clear errors
("this email is already registered") instead of checking first and racing.

Name your constraints (`CONSTRAINT one_invoice_per_period ...`): the name appears in the error
message and in migrations. A `CHECK` sees **one row** only; rules across rows or tables need a
UNIQUE, an exclusion constraint, a foreign key or, as a last resort, a trigger. And remember
three-valued logic: `CHECK (seats > 0)` lets a NULL through, so pair it with `NOT NULL`.

## Keys

- A **composite key** spans columns: `PRIMARY KEY (order_id, product_id)` on an order line,
  or `UNIQUE (account_id, period)` above. Order matters for the index behind it (Phase 3).
- **Surrogate vs natural.** A surrogate key (`bigint` identity, UUID) never changes and is
  small; a natural key (email, ISBN) carries meaning but can change. A common design: a
  surrogate primary key and the natural key as `UNIQUE NOT NULL`.
- **UUIDs** can be created by clients and do not reveal counts, but random UUIDs (version 4)
  scatter inserts across the index; time-ordered UUIDs (version 7, `uuidv7()` in PostgreSQL 18)
  avoid that.
- Use `bigint` for ids of tables that grow: running out of `integer` (2.1 billion) is an
  outage.

## Foreign keys and their actions

What happens to `invoices` when its account is deleted or its key changes?

| Action | On delete of the parent | Typical use |
| --- | --- | --- |
| `NO ACTION` (default) / `RESTRICT` | reject the delete while children exist | history that must stay |
| `CASCADE` | delete the children too | parts that live only inside the parent (order lines) |
| `SET NULL` | set the child's column to NULL | an optional link (an assignee who left) |
| `SET DEFAULT` | set it to its default | rare |

`ON UPDATE CASCADE` is the same idea for key changes. `CASCADE` can delete far more than you
expect through chains of tables; be deliberate. Index the referencing column
(`invoices.account_id`): PostgreSQL does not do it for you, and without the index every delete
of a parent scans the child table.

MySQL's MyISAM ignores foreign keys (use InnoDB); SQLite checks them only after
`PRAGMA foreign_keys = ON`.

## Deferred checks

Constraints are checked at the end of each **statement**. Some changes are only consistent at
the end of the **transaction**, such as swapping two unique positions or inserting rows that
reference each other. Declare the constraint `DEFERRABLE` and defer it:

```sql
ALTER TABLE playlist_items
  ADD CONSTRAINT unique_position UNIQUE (playlist_id, position) DEFERRABLE INITIALLY IMMEDIATE;

BEGIN;
SET CONSTRAINTS unique_position DEFERRED;
UPDATE playlist_items SET position = 2 WHERE id = 10;
UPDATE playlist_items SET position = 1 WHERE id = 11;
COMMIT;    -- checked here
```

## More guarantees

```sql
-- no two bookings of a room overlap (PostgreSQL, needs the btree_gist extension)
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE bookings (
    room_id integer NOT NULL REFERENCES rooms (id),
    during  tstzrange NOT NULL,
    EXCLUDE USING gist (room_id WITH =, during WITH &&)
);

-- a column always computed from others
ALTER TABLE order_items
  ADD COLUMN line_total numeric(12, 2) GENERATED ALWAYS AS (quantity * unit_price) STORED;

-- unique among active rows only: a partial unique index
CREATE UNIQUE INDEX one_active_email ON users (lower(email)) WHERE deleted_at IS NULL;
```

The overlapping-bookings rule is the classic one that application checks get wrong: two
requests read "free" at the same moment and both insert.

## Adding constraints to tables with data

`ALTER TABLE ... ADD CONSTRAINT` checks every existing row while holding a lock that blocks
writes. On a large table, split it:

```sql
ALTER TABLE invoices ADD CONSTRAINT amount_non_negative CHECK (amount >= 0) NOT VALID;  -- new rows only, fast
ALTER TABLE invoices VALIDATE CONSTRAINT amount_non_negative;   -- scans, but lets writes continue
```

The same works for foreign keys. For `NOT NULL` on PostgreSQL, add a validated
`CHECK (col IS NOT NULL)` first; `SET NOT NULL` then uses it and skips the scan. Build unique
indexes with `CREATE UNIQUE INDEX CONCURRENTLY` and attach them with
`ADD CONSTRAINT ... UNIQUE USING INDEX`.

## Summary

- Constraints are checked by the database on every write: the only rules that cannot be
  bypassed or raced.
- NOT NULL, CHECK, UNIQUE, PRIMARY KEY, FOREIGN KEY and EXCLUDE; violations are SQLSTATE class 23.
- CHECK lets NULL through; pair it with NOT NULL. Name your constraints.
- Choose ON DELETE deliberately and index foreign key columns.
- On big tables, add constraints NOT VALID and VALIDATE them later.

## Further reading

- PostgreSQL 17, "Constraints": <https://www.postgresql.org/docs/17/ddl-constraints.html>
- PostgreSQL 17, "CREATE TABLE": <https://www.postgresql.org/docs/17/sql-createtable.html>
- PostgreSQL 17, "Generated Columns": <https://www.postgresql.org/docs/17/ddl-generated-columns.html>
- Modern SQL, "Structured Primary Keys": <https://modern-sql.com/blog/2026-06/structured-primary-keys>
- roadmap.sh SQL topics (constraints, keys): <https://github.com/nilbuild/developer-roadmap/tree/master/roadmaps/sql/content>
