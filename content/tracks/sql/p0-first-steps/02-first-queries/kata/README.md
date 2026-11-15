# First Queries

The shop's `products` table (see `fixture/01_products.sql`):

| Column | Type | Example |
| --- | --- | --- |
| `id` | integer | `1` |
| `name` | text | `Bamboo Sword` |
| `category` | text | `training` |
| `price` | numeric(8, 2) | `24.90` |
| `stock` | integer | `12` (0 means out of stock) |

Write one query in each file:

| File | Returns | Order |
| --- | --- | --- |
| `all_products.sql` | `name`, `price` of every product | by name, A to Z |
| `cheap_products.sql` | `name`, `price` of the products with a price **below** 20.00 that are in stock | cheapest first; equal prices by name |
| `categories.sql` | each `category` once | alphabetical |

Return exactly the columns asked for, in that order. The names of the columns do not
matter.

Try your queries before saving, with nothing to install. From the kata folder:

```bash
docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play
```

Then, in psql: `\d products`, `SELECT * FROM products;`, `\i cheap_products.sql`.

Tools: `SELECT col1, col2`, `FROM`, `WHERE ... AND ...`, `<`, `>`, `ORDER BY a, b`,
`SELECT DISTINCT`.

There are 7 tests: 1 in `tests/visible.sql` (open it to see how SQL katas are tested) and
6 hidden ones. Some hidden tests add or remove rows first, so write queries that answer the
question, not queries that list the expected names.
