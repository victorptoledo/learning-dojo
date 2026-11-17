# Sidequest: Fast catalog

An online shop has 100,000 products and 200,000 reviews, and its pages got slow. You make
them fast the way it is done in production: write the query, read its plan, add the index
that plan needs, and prove the gain.

```text
categories  (id, name)
products    (id, sku, name, category_id → categories, brand, price, stock, active, created_at)
reviews     (id, product_id → products, rating 1-5, created_at)
page_cursor (created_at, id)   -- where the next page starts (stage 3)
```

`fixture/` builds the same data on every check and adds the shop's old indexes. Over four
stages you write the queries of the shop's pages, one per file, and **`indexes.sql`**, the
`CREATE INDEX` statements that make them fast; you add to it in every stage.

How it is checked:

- Each query must return the right rows; the tests compare them with the expected ones,
  also after changing the data.
- Then the tests run `indexes.sql` and measure each query with
  `EXPLAIN (ANALYZE, BUFFERS)`: the 8 kB pages it reads must stay within a **buffer budget**.
  A sequential scan of `products` reads about 1,200 pages; a good index plan reads a few
  dozen at most. When a query is over its budget, the test shows its plan.
- Some tests also look at the plan itself (no `Sort`, an `Index Only Scan`), or at the
  indexes that exist.
- Write plain statements: no `BEGIN`/`COMMIT`, no `CONCURRENTLY` (each test runs inside a
  transaction that is rolled back), no `psql` commands.
- Try your files with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`
  from this folder: `\i indexes.sql`, then `EXPLAIN (ANALYZE, BUFFERS) ...` your query.

When you are done, `[A]` copies a review prompt: ask an AI whether each index earns its keep.
