The filters on the side of the search page. Write the two queries and add their indexes to
`indexes.sql`.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `pool_and_sea.sql` | `id`, `title`, `price` | the listings with **both** a `pool` and a `sea-view` among their `features`, where `attrs` says `"pets": true`; cheapest first, then by `id` | 100 buffers |
| `top_floor_a.sql` | `id`, `city`, `price` | the listings whose `attrs` have `"energy": "A"` **and** `"floor": 20`; by `id` | 200 buffers |

Write the conditions with the containment operator `@>`: on arrays, `features @>
ARRAY['pool', 'sea-view']` ("contains all of these"); on JSONB, `attrs @> '{"energy": "A"}'`
("contains this piece of document"). Both questions are about what is **inside** a value, and
a B-tree, which sorts whole values, cannot answer them.

> [!TIP]
> A GIN (generalized inverted) index stores, for every element of every array or every key
> and value of every document, the list of rows that hold it, like the index at the back of a
> book. `@>` then intersects a few lists. For `jsonb` there are two operator classes: the
> default `jsonb_ops` and the smaller `jsonb_path_ops`, which only supports containment and
> jsonpath. Compare their sizes with `pg_relation_size`, and read the plan: a `Bitmap Index
> Scan` finds the rows, then a `Bitmap Heap Scan` reads only the pages that hold them.
