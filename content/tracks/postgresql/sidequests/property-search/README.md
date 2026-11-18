# Sidequest: Property search

A property portal has 100,000 listings and a log of a million asking prices, and every search
on the site reads the whole table. B-tree indexes do not help: the searches ask "does this
array contain these values?", "does this title contain this text, or something like it?",
"what is closest to this point?". PostgreSQL has an index for each of those questions. You
write the searches and the indexes, in four stages:

1. features (a `text[]`) and attributes (`jsonb`) with GIN;
2. substrings and typos in titles with trigrams (`pg_trgm`);
3. the nearest listings and those within a radius with GiST;
4. a week or a month of prices out of ten years, with a BRIN index of a few kilobytes.

```text
properties    (id, title, city, kind, bedrooms, price, features text[], attrs jsonb,
               location point, listed_at)                          -- 100,000 rows
price_history (property_id → properties, observed_at, price)      -- 1,000,000 rows
```

`fixture/01_properties.sql` creates and fills them, the same way on every check. A title reads
like `Cozy loft in Ribeta`; `attrs` holds `floor`, `pets`, `energy` (A to G) and `furnished`;
`location` is a point on a 100 × 100 km map.

How it is checked:

- **One file of indexes, `indexes.sql`**, that every stage adds to: `CREATE INDEX` and
  `CREATE EXTENSION` statements. Change no data and no table. The dojo loads it once,
  commits it, and runs `VACUUM ANALYZE`, as autovacuum would have done long ago.
- **One file per search**, holding one query. Each must return the right rows, and stay
  within its **buffer budget**: the shared buffers `EXPLAIN (ANALYZE, BUFFERS)` reports, which
  is the work the plan does. A sequential scan of `properties` reads about 2,800. When a
  query is over budget, the test shows its plan.
- Try things with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play` from
  this folder: `\i indexes.sql`, `VACUUM ANALYZE;`, then `EXPLAIN (ANALYZE, BUFFERS)` your
  query.

When you are done, `[A]` copies a review prompt: ask an AI what each index costs on every
insert, and which one you could do without.
