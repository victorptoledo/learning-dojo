# Relational Modelling, SQL and Indexes

ORMs write SQL for you, but they cannot design your schema or choose your indexes. A backend
developer reads and writes SQL, models data so the database can protect it, and knows why a query
takes one millisecond or ten seconds.

## Tables, keys and constraints

```sql
create table customers (
    id          bigint generated always as identity primary key,
    email       varchar(320) not null unique,
    name        varchar(100) not null,
    created_at  timestamptz  not null default now()
);

create table orders (
    id           bigint generated always as identity primary key,
    customer_id  bigint not null references customers (id),
    status       varchar(20) not null check (status in ('PLACED', 'PAID', 'SHIPPED', 'CANCELLED')),
    total_cents  bigint not null check (total_cents >= 0),
    placed_at    timestamptz not null default now()
);

create table order_lines (
    order_id    bigint not null references orders (id) on delete cascade,
    sku         varchar(20) not null,
    quantity    int not null check (quantity > 0),
    unit_cents  bigint not null,
    primary key (order_id, sku)
);
```

- A **primary key** identifies each row. A surrogate key (`id`) is simple and stable; a
  **composite** key (`order_id, sku`) fits link tables.
- A **foreign key** (`references`) guarantees the related row exists.
- `not null`, `unique` and `check` let the database refuse bad data even when application code
  has a bug. Constraints are the last line of defence; use them.

## Relationships and normalisation

| Relationship | How |
| --- | --- |
| one-to-many (customer → orders) | a foreign key on the "many" side |
| many-to-many (students ↔ courses) | a link table with two foreign keys |
| one-to-one | a foreign key with `unique`, or the same primary key |

**Normalise** first: each fact in one place. If the customer's email is copied into every order,
changing it means updating many rows and forgetting one. **Denormalise** deliberately when a
measured read path needs it (a cached total, a reporting table), and keep it in sync on purpose.

## Queries you will write every day

```sql
-- join: orders with their customer
select o.id, c.email, o.total_cents
from orders o
join customers c on c.id = o.customer_id
where o.status = 'PAID'
order by o.placed_at desc
limit 20;

-- aggregate: revenue per customer, only the big ones
select c.email, count(*) as orders, sum(o.total_cents) as revenue
from orders o
join customers c on c.id = o.customer_id
group by c.email
having sum(o.total_cents) > 100000
order by revenue desc;

-- customers without any order
select c.*
from customers c
where not exists (select 1 from orders o where o.customer_id = c.id);
```

- `join` keeps matching rows; `left join` keeps every row of the left table, with nulls when there
  is no match.
- `where` filters rows before grouping; `having` filters groups after it.
- `exists` stops at the first match; prefer it to `in (select ...)` over large tables.

## Indexes

Without an index, `where customer_id = 42` reads **every** row (a sequential scan). An index (a
B-tree, by default) is a sorted structure the database can search in a few steps:

```sql
create index orders_customer_idx on orders (customer_id);
create index orders_status_placed_idx on orders (status, placed_at desc);
```

What to index:

- foreign keys you join or filter on (PostgreSQL does **not** index them automatically);
- columns in frequent `where`, `order by` and `join` conditions;
- **composite** indexes for combined filters, column order matters: `(status, placed_at)` serves
  `where status = ? order by placed_at`, and also `where status = ?`, but not `where placed_at > ?`
  alone.

Indexes are not free: each one slows writes and uses space. Add them for real queries, and remove
unused ones.

## Reading a query plan

`explain analyze` shows what the database actually did:

```text
explain analyze select * from orders where customer_id = 42;

Index Scan using orders_customer_idx on orders  (cost=0.29..8.31 rows=5 width=48)
                                                (actual time=0.020..0.024 rows=5 loops=1)
```

`Seq Scan` on a large table in a frequent query is the classic warning sign. Also watch the
estimated versus actual row counts: a large gap means stale statistics (run `analyze`).

## SQL injection, once and for all

Never build SQL by concatenating input:

```java
// vulnerable: name = "x' or '1'='1"
"select * from users where name = '" + name + "'"

// safe: the value is sent separately and never parsed as SQL
jdbc.query("select * from users where name = ?", mapper, name);
```

JPA named parameters (`:name`), `PreparedStatement` and Spring's `JdbcClient` all bind values
this way.

## Summary

- Primary keys, foreign keys, `not null`, `unique` and `check` let the database protect the data.
- Normalise first, denormalise deliberately.
- Know `join`, `left join`, `group by`/`having` and `exists`.
- Index foreign keys and frequent filters; composite index order matters; every index costs writes.
- Read plans with `explain analyze`; a sequential scan on a big table is a warning.
- Always bind parameters.
