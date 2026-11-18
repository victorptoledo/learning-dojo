# Sidequest: Price book

A small shop keeps its catalogue and prices in PostgreSQL, and several programs write to it:
the back office, an import script, the website. Every rule that lives only in one of those
programs is broken by the others sooner or later. So this time the **database** holds the
rules, with the types PostgreSQL has and most databases do not. You build it in three stages:

1. products with a domain for the SKU, enums for the status and the currency, a tag array
   with rules of its own, and discount rules stored as JSONB that the table checks;
2. a price history in ranges of time that never overlap and never leave a gap, and the price
   a customer pays;
3. a view counter that takes a click per page view without bloating the table or losing a
   click when two sessions count at once.

```text
products      (id, sku, name, status, tags)                    -- stage 1
discounts     (id, product_id → products, rule, kind)          -- stage 1
prices        (product_id → products, currency, amount, valid) -- stage 2
product_stats (product_id → products, views, last_viewed_at)   -- stage 3
```

How it is checked:

- There is no fixture: you create everything, one file per stage (`catalog.sql`,
  `prices.sql`, `views.sql`). Each stage's statement gives the names, the rules and the
  functions the tests use; how you write them is yours.
- The dojo loads each file once and **commits** it, then runs the tests. Most tests insert
  rows and call your functions inside a transaction that is rolled back; the last stage also
  opens two real sessions (with `dblink`) that click at the same time.
- So the files must load in one go on an empty database: no `BEGIN`/`COMMIT`, no `psql`
  commands. Later stages may change earlier files.
- An insert the rules forbid must fail with the right SQLSTATE: `23514` (a `CHECK`, also a
  domain's), `23505` (unique), `23P01` (exclusion), `22P02` (a value an enum does not have).
- Try things with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` from
  this folder, then `\i catalog.sql` and insert rows by hand.

When you are done, `[A]` copies a review prompt: ask an AI which rule a careless program could
still get past.
