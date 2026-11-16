# Fix the SQL Injection

`store.py` is the data-access layer of a small shop on SQLite (`sqlite3` from the standard
library). `seed.py` creates the schema and some products for the tests; do not change it.
Every function in `store.py` builds its SQL with f-strings. Make each one safe:

| Function | Returns | What must hold |
| --- | --- | --- |
| `find_user(conn, username)` | the user as `{"id", "username", "role"}`, or `None` | the name is matched exactly, as data |
| `search_products(conn, term, *, sort="name", descending=False, limit=20)` | a list of `{"id", "name", "price_cents"}` whose name contains `term` | see below |
| `products_by_ids(conn, ids)` | the products with those ids, in increasing id order | one placeholder per id; `[]` for an empty list |
| `rename_product(conn, product_id, new_name)` | `True` if a row changed, else `False` | both values bound |

`search_products`:

- `term` matches **literally**, ignoring case for ASCII letters (SQLite's `LIKE` does that):
  `%` and `_` in the term are ordinary characters, not wildcards. Escape them and use
  `LIKE ? ESCAPE '\'`.
- `sort` is one of `"name"`, `"price"` (the `price_cents` column) or `"newest"` (the
  `created_at` column); anything else raises `ValueError`. `descending=True` reverses the
  order. Rows with equal sort values are ordered by `id`.
- `limit` must be an `int` (not a `bool`, not a string); anything else raises `ValueError`.
  Values below 1 become 1 and values above 100 become 100.

Honest results must stay as they are now: the tests compare them with what the current code
returns for normal input. And none of the payloads in the tests may change data or read rows
it should not.

> [!NOTE]
> This kata runs offline against an in-memory database. It is about fixing your own code,
> not attacking anyone else's.

Run the visible tests with `uv run pytest`. There are 13 tests: 2 in
`tests/test_visible.py` and 11 hidden ones, plus ruff on `store.py` and `seed.py`.
