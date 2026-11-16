# PostgreSQL Types: Arrays, Ranges, Enums, Domains and More

The SQL track's lesson
[The Relational Model: Tables, Keys and Data Types](../../../sql/p1-fundamentals/01-relational-model/lesson.md)
covers the standard types: integers, `numeric`, `text`, dates and identity columns. PostgreSQL
has many more, and some of them replace whole tables or application code. This lesson is about
the ones you will meet in real schemas, and when **not** to use them.

## A few standard types, the PostgreSQL way

| Need | Use | Avoid, and why |
| --- | --- | --- |
| strings | `text` (or `varchar(n)` when the limit is a real rule) | `char(n)`: pads with spaces. `varchar(255)` "for speed": all three are stored the same way |
| money | `numeric(12,2)` or integer cents | `money` (depends on `lc_monetary`), `float` (inexact) |
| points in time | `timestamptz` | `timestamp` (without time zone) for events: it stores a wall clock, not an instant |
| keys | `bigint GENERATED ALWAYS AS IDENTITY` | `serial` (older: a sequence the table does not really own) |
| random keys | `uuid` with `gen_random_uuid()` (v4, built in since 13) | text UUIDs: 36 bytes instead of 16 |

`timestamptz` does **not** store a time zone. It stores an instant (UTC microseconds) and converts
on input and output using the session's `TimeZone` setting:

```sql
SET TimeZone = 'America/Sao_Paulo';
SELECT TIMESTAMPTZ '2026-11-15 12:00+00';        -- 2026-11-15 09:00:00-03
SELECT TIMESTAMPTZ '2026-11-15 12:00+00' AT TIME ZONE 'Europe/Lisbon';   -- 2026-11-15 12:00:00 (a timestamp)
```

Random v4 UUIDs scatter inserts across the whole primary key index, which hurts caching on big
tables. Time-ordered **UUIDv7** keys avoid that; PostgreSQL 18 adds `uuidv7()`, and on 17 you
generate them in the application.

## Arrays

Any type can be an array: `text[]`, `integer[]`, `date[]`.

```sql
CREATE TABLE articles (id bigint PRIMARY KEY, title text, tags text[] NOT NULL DEFAULT '{}');
INSERT INTO articles VALUES (1, 'MVCC', ARRAY['postgres', 'internals']), (2, 'Joins', '{sql}');

SELECT * FROM articles WHERE 'postgres' = ANY (tags);      -- contains one value
SELECT * FROM articles WHERE tags @> ARRAY['postgres'];     -- contains all of these (GIN-indexable)
SELECT * FROM articles WHERE tags && ARRAY['sql', 'go'];    -- overlaps (any in common)
SELECT id, unnest(tags) AS tag FROM articles;               -- one row per element
SELECT array_agg(title ORDER BY title) FROM articles;       -- rows back into an array
SELECT cardinality(tags), tags[1] FROM articles;            -- arrays start at 1
```

Arrays are good for **small lists that you read with the row** (tags, a few phone numbers). They
are wrong when the elements are things with their own data or need foreign keys: an array of
`product_id`s cannot reference `products`. Use a join table then.

## Enums

```sql
CREATE TYPE order_status AS ENUM ('new', 'paid', 'shipped', 'cancelled');
ALTER TYPE order_status ADD VALUE 'refunded' AFTER 'shipped';
```

Enums are compact (4 bytes), sort in declaration order and reject unknown values. But a value can
**never be removed or renamed easily** (renaming works with `ALTER TYPE ... RENAME VALUE`; removal
needs a new type and a table rewrite). Choose:

| Option | Good when |
| --- | --- |
| `ENUM` | a short, stable list owned by the code (days of the week, a state machine) |
| `text` + `CHECK (status IN (...))` | the list may change; changing it is one `ALTER TABLE` |
| a lookup table with a foreign key | the values have attributes (a label, an order, an "active" flag) or are edited by users |

## Domains

A **domain** is a type with rules, declared once and reused by every column:

```sql
CREATE DOMAIN email AS text CHECK (VALUE ~ '^[^@\s]+@[^@\s]+$');
CREATE DOMAIN positive_money AS numeric(12,2) NOT NULL CHECK (VALUE > 0);

CREATE TABLE invoices (id bigint PRIMARY KEY, billed_to email, amount positive_money);
```

Changing the rule later (`ALTER DOMAIN ... ADD CONSTRAINT`) checks every column that uses it.

## Ranges and multiranges

A **range** is a pair of bounds with an inclusive `[` or exclusive `)` end: `int4range`,
`int8range`, `numrange`, `daterange`, `tsrange`, `tstzrange`. Half-open `[start, end)` is the
default and the right choice for time: 10:00–11:00 and 11:00–12:00 touch but do not overlap.

```sql
SELECT tstzrange('2026-11-15 10:00+00', '2026-11-15 11:00+00');        -- ["...10:00+00","...11:00+00")
SELECT daterange('2026-11-01', '2026-11-08') @> DATE '2026-11-05';     -- true: contains
SELECT int4range(1, 5) && int4range(5, 9);                              -- false: [1,5) and [5,9) only touch
SELECT int4range(1, 5) * int4range(3, 9);                               -- [3,5): intersection
SELECT lower(r), upper(r), upper(r) - lower(r) FROM (SELECT daterange('2026-11-01', '2026-11-08') AS r) AS t;
SELECT tstzrange('2026-11-15 10:00+00', NULL);                          -- unbounded: "from 10:00 on"
```

Multiranges (PostgreSQL 14+) hold several ranges: `int4multirange`, `tstzmultirange`. `range_agg`
merges ranges into one multirange, which answers "when was this room busy?" in one call.

### Exclusion constraints: no double bookings

A unique constraint forbids two **equal** values. An **exclusion constraint** forbids two rows
whose values satisfy an operator, such as `&&` (overlap):

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;     -- lets GiST index plain = on room_id

CREATE TABLE bookings (
    id      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id integer NOT NULL,
    during  tstzrange NOT NULL CHECK (NOT isempty(during)),
    EXCLUDE USING gist (room_id WITH =, during WITH &&)
);

INSERT INTO bookings (room_id, during) VALUES (1, '[2026-11-15 10:00+00, 2026-11-15 11:00+00)');
INSERT INTO bookings (room_id, during) VALUES (1, '[2026-11-15 10:30+00, 2026-11-15 12:00+00)');
-- ERROR:  conflicting key value violates exclusion constraint "bookings_room_id_during_excl"  (SQLSTATE 23P01)
```

Read it as: "no two rows may have equal `room_id` **and** overlapping `during`". It is checked
by the database for every concurrent transaction, which an application check (`SELECT` then
`INSERT`) can never guarantee. Add a `WHERE (status <> 'cancelled')` to the `EXCLUDE` clause to
ignore some rows.

## Generated columns

```sql
CREATE TABLE order_lines (
    quantity   integer NOT NULL,
    unit_price numeric(10,2) NOT NULL,
    line_total numeric(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED
);
```

A stored generated column is computed on every insert and update and can be indexed. The
expression must be **immutable**: it cannot call `now()` or depend on another table. PostgreSQL
18 adds `VIRTUAL` generated columns, computed when read.

## Other types worth knowing

| Type | For |
| --- | --- |
| `inet`, `cidr` | IP addresses and networks, with `<<` "is contained in" (`'10.0.0.7' << '10.0.0.0/24'`) |
| `interval` | durations: `INTERVAL '90 minutes'`, `now() - created_at` |
| `bytea` | binary data (small; big files belong in object storage) |
| `jsonb` | documents (lesson [JSONB and Full-Text Search](../../p2-intermediate/05-jsonb-full-text/lesson.md)) |
| `tsvector` | searchable text (same lesson) |
| composite types | a row type (`CREATE TYPE address AS (street text, city text)`), mostly for function results |

## Summary

- `text`, `numeric`, `timestamptz` and identity columns cover most columns; `timestamptz`
  stores an instant and shows it in the session's time zone.
- Arrays for small lists read with the row; a join table when elements need keys or data.
- Enums for stable lists, `CHECK` for lists that change, lookup tables for values with
  attributes.
- Domains put a rule in one place.
- Ranges model periods; use half-open `[)` bounds, and an exclusion constraint with `&&` to
  forbid overlaps (with `btree_gist` for the `=` part).
- Stored generated columns compute values from other columns of the same row.

## Further reading

- PostgreSQL 17, "Data Types": <https://www.postgresql.org/docs/17/datatype.html>
- PostgreSQL 17, "Arrays": <https://www.postgresql.org/docs/17/arrays.html>
- PostgreSQL 17, "Range Types" (with exclusion constraints): <https://www.postgresql.org/docs/17/rangetypes.html>
- PostgreSQL 17, "CREATE DOMAIN": <https://www.postgresql.org/docs/17/sql-createdomain.html>
- PostgreSQL 17, "Enumerated Types": <https://www.postgresql.org/docs/17/datatype-enum.html>
- PostgreSQL 17, "Generated Columns": <https://www.postgresql.org/docs/17/ddl-generated-columns.html>
- PostgreSQL wiki, "Don't Do This" (types to avoid): <https://wiki.postgresql.org/wiki/Don%27t_Do_This>
