# PL/pgSQL Functions, Procedures and Triggers

PostgreSQL runs code next to the data: functions in SQL or PL/pgSQL (and Python, Perl or Tcl
through extensions), procedures that can commit, and triggers that fire on every change. Used
well, they keep rules in one place that every client must follow (an audit trail, an
`updated_at` column, a counter). Used badly, they hide business logic where nobody looks. This
lesson covers the language, the attributes that matter for performance and security, and the
trigger patterns worth knowing.

## SQL functions and PL/pgSQL functions

```sql
-- A SQL function: one or more SQL statements; the planner can often inline it.
CREATE FUNCTION order_total(p_order bigint) RETURNS numeric
LANGUAGE sql STABLE
AS $$
    SELECT coalesce(sum(quantity * unit_price), 0) FROM order_lines WHERE order_id = p_order;
$$;

-- A PL/pgSQL function: variables, branches, loops and error handling.
CREATE FUNCTION apply_discount(p_order bigint, p_percent numeric) RETURNS numeric
LANGUAGE plpgsql
AS $$
DECLARE
    v_total numeric;
BEGIN
    IF p_percent NOT BETWEEN 0 AND 50 THEN
        RAISE EXCEPTION 'discount % out of range', p_percent USING ERRCODE = '22023';
    END IF;
    UPDATE orders SET discount = p_percent WHERE id = p_order
    RETURNING total * (1 - p_percent / 100) INTO v_total;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'order % not found', p_order USING ERRCODE = 'P0002';
    END IF;
    RETURN v_total;
END
$$;
```

The body is a string; `$$ ... $$` (dollar quoting) avoids escaping quotes inside it. Prefer a
plain **SQL function** when the logic is a query: it is simpler and can be inlined into the
calling query.

### The PL/pgSQL you need

| Construct | Example |
| --- | --- |
| variables | `DECLARE v_count integer := 0; r record;` |
| one row into variables | `SELECT a, b INTO v_a, v_b FROM t WHERE id = p_id;` (`INTO STRICT` raises if not exactly one row) |
| did it find something? | `IF NOT FOUND THEN ...` after `SELECT INTO`, `UPDATE`, `DELETE` |
| rows affected | `GET DIAGNOSTICS v_rows = ROW_COUNT;` |
| loop over a query | `FOR r IN SELECT * FROM t LOOP ... END LOOP;` |
| return a set | `RETURNS TABLE (id bigint, name text)` with `RETURN QUERY SELECT ...;` |
| run and ignore a result | `PERFORM pg_notify('orders', p_id::text);` |
| messages and errors | `RAISE NOTICE 'x = %', x;` / `RAISE EXCEPTION '...' USING ERRCODE = '...', HINT = '...';` |
| catch errors | `BEGIN ... EXCEPTION WHEN unique_violation THEN ... END;` |
| dynamic SQL | `EXECUTE format('SELECT count(*) FROM %I WHERE status = %L', tbl, st) INTO v_n;` or `EXECUTE '...' USING p1;` |

Two warnings:

- A block with an `EXCEPTION` clause starts a **subtransaction** each time it is entered. In a
  loop over a million rows that is slow and burns subtransaction ids; catch errors around the
  loop, not inside it, when you can.
- In dynamic SQL, never concatenate values: use `format('%I', name)` for identifiers,
  `%L` or `USING` for values. Otherwise it is SQL injection inside the database.

## Attributes that matter

| Attribute | Means | Why it matters |
| --- | --- | --- |
| `IMMUTABLE` | same arguments, same result, forever; reads no tables | required for index expressions and generated columns; the planner may evaluate it once |
| `STABLE` | same result within one statement; may read tables | can be used in index scans' conditions (`WHERE created_at > some_stable_fn()`) |
| `VOLATILE` (default) | may change anything, any time (`random()`, writes) | called again for every row |
| `STRICT` | returns NULL at once if any argument is NULL | skips the body |
| `PARALLEL SAFE` | can run in parallel workers | without it, a query calling the function cannot use a parallel plan |
| `SECURITY DEFINER` | runs with the **owner's** privileges | lets users do one controlled thing they could not do directly |
| `SET search_path = ...` | fixes the search path while it runs | mandatory with `SECURITY DEFINER` |

Lying about volatility is a real bug: marking a function that reads a table `IMMUTABLE` lets the
planner cache a stale result or build an index that later disagrees with the table.

A `SECURITY DEFINER` function must pin its `search_path` (`SET search_path = pg_catalog, public`
or schema-qualify everything); otherwise a caller can create a table or operator with the same
name in a schema that comes first and make the function run their code with the owner's rights.
`REVOKE EXECUTE ... FROM PUBLIC` and grant it only to the roles that need it.

## Procedures

A procedure (`CREATE PROCEDURE`, called with `CALL`) can **commit inside its body**, which a
function cannot. Use it for batch jobs that should commit as they go:

```sql
CREATE PROCEDURE purge_old_events(p_batch integer DEFAULT 10000)
LANGUAGE plpgsql AS $$
DECLARE
    v_rows integer;
BEGIN
    LOOP
        DELETE FROM events WHERE id IN (
            SELECT id FROM events WHERE at < now() - INTERVAL '90 days' LIMIT p_batch);
        GET DIAGNOSTICS v_rows = ROW_COUNT;
        COMMIT;                 -- short transactions: locks released, vacuum can keep up
        EXIT WHEN v_rows < p_batch;
    END LOOP;
END
$$;
CALL purge_old_events();
```

A `DO $$ ... $$` block runs anonymous PL/pgSQL once, for scripts and migrations.

## Triggers

A trigger runs a **trigger function** on `INSERT`, `UPDATE`, `DELETE` or `TRUNCATE` of a table:

| Choice | Options |
| --- | --- |
| when | `BEFORE` (can change or skip the row), `AFTER` (sees the final row; can fail the statement), `INSTEAD OF` (views only) |
| granularity | `FOR EACH ROW` (once per row, with `NEW` and `OLD`) or `FOR EACH STATEMENT` (once, optionally with transition tables) |
| filter | `WHEN (OLD.status IS DISTINCT FROM NEW.status)` skips the call entirely when false |
| columns | `UPDATE OF status, total` fires only when those columns are in the `SET` list |

Inside a row trigger function: `NEW` and `OLD` (NULL when they do not apply), `TG_OP`
(`'INSERT'`, `'UPDATE'`, `'DELETE'`), `TG_TABLE_NAME`, `TG_WHEN`. A `BEFORE` row trigger
returns the row to write (`NEW`, possibly changed) or `NULL` to skip it; an `AFTER` trigger's
return value is ignored (return `NULL`).

### Pattern: updated_at

```sql
CREATE FUNCTION set_updated_at() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END
$$;

CREATE TRIGGER orders_updated_at BEFORE UPDATE ON orders
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

### Pattern: an audit trail

```sql
CREATE TABLE audit_log (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    at         timestamptz NOT NULL DEFAULT now(),
    actor      text NOT NULL DEFAULT current_user,
    table_name text NOT NULL,
    op         text NOT NULL,
    row_id     bigint NOT NULL,
    old_row    jsonb,
    new_row    jsonb
);

CREATE FUNCTION audit() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO audit_log (table_name, op, row_id, old_row, new_row)
    VALUES (TG_TABLE_NAME, TG_OP,
            CASE WHEN TG_OP = 'DELETE' THEN OLD.id ELSE NEW.id END,
            CASE WHEN TG_OP <> 'INSERT' THEN to_jsonb(OLD) END,
            CASE WHEN TG_OP <> 'DELETE' THEN to_jsonb(NEW) END);
    RETURN NULL;
END
$$;

CREATE TRIGGER orders_audit AFTER INSERT OR UPDATE OR DELETE ON orders
FOR EACH ROW EXECUTE FUNCTION audit();
```

`to_jsonb(OLD)` turns a whole row into a document, so one function audits any table. To store
only what changed, compare the two documents key by key (`jsonb_each`).

### Pattern: statement triggers with transition tables

A row trigger on a 100,000-row `UPDATE` runs 100,000 times. A statement trigger with
**transition tables** runs once and sees all changed rows as tables:

```sql
CREATE TRIGGER orders_audit_bulk AFTER UPDATE ON orders
REFERENCING OLD TABLE AS old_rows NEW TABLE AS new_rows
FOR EACH STATEMENT EXECUTE FUNCTION audit_bulk();
-- inside: INSERT INTO audit_log (...) SELECT ... FROM new_rows JOIN old_rows USING (id);
```

### Trigger pitfalls

- Triggers are **invisible** to someone reading the application code. Name them clearly, keep
  them small, and document them (`COMMENT ON TRIGGER`).
- A trigger that updates its own table fires itself again; guard with a `WHEN` clause or
  `pg_trigger_depth()`.
- Triggers fire in **alphabetical order** by name for the same event.
- Row triggers make bulk loads slow; `COPY` fires them too.
- `TRUNCATE` does not fire `DELETE` triggers.
- Business rules that need data from other services do not belong in triggers.

**Event triggers** (`CREATE EVENT TRIGGER`) fire on DDL (`ddl_command_end`, `sql_drop`), for
example to forbid `DROP TABLE` in production or to log schema changes.

## Summary

- Prefer SQL functions for queries; PL/pgSQL for logic with variables, branches, loops and errors.
- Know `INTO`, `FOUND`, `GET DIAGNOSTICS`, `RETURN QUERY`, `RAISE ... USING ERRCODE` and
  `EXCEPTION` blocks (each one a subtransaction).
- Mark volatility honestly; `IMMUTABLE` is required for index expressions and generated columns.
- `SECURITY DEFINER` needs a fixed `search_path` and restricted `EXECUTE`.
- Procedures can `COMMIT`: use them for batch jobs.
- Row triggers with `NEW`/`OLD`/`TG_OP` for `updated_at` and audit trails; `WHEN` and `UPDATE OF`
  to fire less; statement triggers with transition tables for bulk changes.

## Further reading

- PostgreSQL 17, "PL/pgSQL — SQL Procedural Language": <https://www.postgresql.org/docs/17/plpgsql.html>
- PostgreSQL 17, "CREATE FUNCTION": <https://www.postgresql.org/docs/17/sql-createfunction.html>
- PostgreSQL 17, "Function Volatility Categories": <https://www.postgresql.org/docs/17/xfunc-volatility.html>
- PostgreSQL 17, "Trigger Functions" in PL/pgSQL: <https://www.postgresql.org/docs/17/plpgsql-trigger.html>
- PostgreSQL 17, "CREATE TRIGGER": <https://www.postgresql.org/docs/17/sql-createtrigger.html>
- PostgreSQL 17, "Writing SECURITY DEFINER Functions Safely": <https://www.postgresql.org/docs/17/sql-createfunction.html#SQL-CREATEFUNCTION-SECURITY>
- PostgreSQL 17, "CREATE PROCEDURE" and transaction control: <https://www.postgresql.org/docs/17/plpgsql-transactions.html>
