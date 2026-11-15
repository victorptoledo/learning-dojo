# Relational Databases, SQL and Your First PostgreSQL

Almost every application you will work on keeps its important data in a **relational
database**: orders, accounts, payments, stock. SQL is the language you use to ask it
questions and to change it. It is more than fifty years old, and it is still the skill that
pays off in every backend, data and analytics job.

## What a database gives you

You could keep data in files and loop over them in code. A **database management system**
(DBMS) gives you what that approach lacks:

| Need | Files and a script | A relational DBMS |
| --- | --- | --- |
| Many users at once | you write the locking yourself | concurrent sessions, isolation |
| Never half a change | a crash leaves files half written | **transactions**: all or nothing |
| Valid data | every program checks (or forgets) | **constraints** checked on every write |
| Fast lookups | you build and maintain your own index | `CREATE INDEX`, used automatically |
| Questions | new code for each question | one query language: SQL |

A **relational** database stores data in **tables** (relations): rows with the same named,
typed columns. Tables refer to each other by **keys**, and queries combine them. Phase 1
looks at the model closely.

## SQL is declarative

You describe **what** result you want, not **how** to compute it:

```sql
SELECT name, email
FROM customers
WHERE country = 'PT'
ORDER BY name;
```

Nothing here says "open the file, loop, compare, sort". The engine's **query planner**
chooses how: read the whole table or use an index, which join algorithm, sort in memory or
on disk. The same query keeps working when the table grows from 100 rows to 100 million;
only the plan changes. Phase 3 teaches you to read those plans.

SQL statements fall into families:

| Family | Statements | Does |
| --- | --- | --- |
| Query | `SELECT` | reads data |
| DML (data manipulation) | `INSERT`, `UPDATE`, `DELETE`, `MERGE` | changes rows |
| DDL (data definition) | `CREATE`, `ALTER`, `DROP` | changes the schema |
| Transaction control | `BEGIN`, `COMMIT`, `ROLLBACK` | groups changes |
| Access control | `GRANT`, `REVOKE` | permissions |

## One standard, many dialects

SQL is an ISO standard (ISO/IEC 9075, latest edition SQL:2023). No engine implements all of
it, and every engine adds its own extensions. The core you will learn here (SELECT, joins,
GROUP BY, subqueries, CTEs, window functions, constraints, transactions) works everywhere,
with small differences:

| Engine | Where you meet it | Notes |
| --- | --- | --- |
| **PostgreSQL** | backends, analytics, the default in many clouds | close to the standard, rich types (JSON, arrays), our reference |
| **MySQL / MariaDB** | web stacks, WordPress, many startups | `LIMIT`, `ON DUPLICATE KEY UPDATE`, looser defaults in old versions |
| **SQL Server** | enterprises on Microsoft stacks | T-SQL: `TOP`, `MERGE`, `IDENTITY`, `[brackets]` |
| **SQLite** | phones, desktop apps, tests | a library in a file, dynamic typing, no server |
| **Oracle** | large enterprises, banks | PL/SQL, `FETCH FIRST`, `''` is NULL |

This track teaches **standard SQL**, runs it on **PostgreSQL 17**, and points out the
differences that bite in practice. Administering PostgreSQL itself (configuration, backups,
replication, vacuum) is the subject of the separate PostgreSQL track.

## Get a PostgreSQL

You need a server and a client. Pick one way:

**1. The dojo image (no install).** The katas run in the `dojo-sql` image (Debian 13 with
PostgreSQL 17). From any kata folder, open a throwaway database with that kata's tables:

```bash
docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play
```

It starts the server, loads the kata's `fixture/` files and opens `psql`. When you quit,
the database is thrown away; your `.sql` files stay in the folder.

**2. The official Docker image**, for a database that keeps its data:

```bash
docker run -d --name pg -e POSTGRES_PASSWORD=dojo -p 5432:5432 -v pgdata:/var/lib/postgresql/data postgres:17
docker exec -it pg psql -U postgres
```

**3. A local install** from <https://www.postgresql.org/download/> (on Windows the EDB
installer includes `psql` and pgAdmin).

## psql in five minutes

`psql` is PostgreSQL's command-line client. Lines that start with a backslash are psql
commands; everything else is SQL sent to the server, ended by `;`.

```text
dojo=# \dt                      -- list tables
dojo=# \d customers             -- describe one: columns, types, indexes, keys
dojo=# SELECT now();            -- any SQL, ended by ;
dojo=# \i cheap_products.sql    -- run a file
dojo=# \x                       -- toggle expanded output for wide rows
dojo=# \timing                  -- show how long each statement takes
dojo=# \?                       -- help on psql commands;  \h SELECT  help on SQL
dojo=# \q                       -- quit
```

A statement without `;` waits for more input (the prompt changes to `dojo-#`). If you are
lost, `Ctrl+C` cancels the current input.

Keywords are case-insensitive (`select` = `SELECT`); the convention is upper case for
keywords and lower case for names. Unquoted names are folded to lower case in PostgreSQL
(the standard folds to upper case), so avoid quoted mixed-case names like `"CustomerName"`.
Strings use **single quotes** `'PT'`; double quotes are for names.

## How the dojo checks SQL katas

A SQL kata is a folder of `.sql` files, one per question, plus `fixture/` with the tables
and data. When you save a file, the dojo runs the `dojo-sql` container (no network) and:

1. starts a fresh PostgreSQL 17 and loads `fixture/*.sql`;
2. runs the tests in `tests/`: each one runs one of your files and compares the rows with
   the expected ones (by value: `10.5` equals `10.50`), or runs your statements and then
   checks the tables;
3. rolls every test back, so tests never see each other's changes.

A starter file holds only a `-- todo: ...` comment, which shows up as "not written yet".
When the order of the rows matters, the statement says so and the test checks it; otherwise
any order passes.

## Summary

- A DBMS gives you concurrency, transactions, constraints, indexes and a query language.
- SQL is declarative: you describe the result; the planner chooses the algorithm.
- The standard is shared, the dialects differ; this track uses PostgreSQL 17 as reference.
- `dojo-sql play` gives you psql on a kata's tables with nothing to install.
- In psql: `\dt`, `\d table`, `\i file.sql`, `\x`, `\timing`, `\q`; SQL ends with `;`.

## Further reading

- PostgreSQL 17 tutorial: <https://www.postgresql.org/docs/17/tutorial.html>
- psql reference: <https://www.postgresql.org/docs/17/app-psql.html>
- roadmap.sh SQL roadmap: <https://roadmap.sh/sql> (topics: <https://github.com/nilbuild/developer-roadmap/tree/master/roadmaps/sql/content>)
- Modern SQL, the standard and its parts: <https://modern-sql.com/standard>
- SQL:2023 overview: <https://en.wikipedia.org/wiki/SQL:2023>
