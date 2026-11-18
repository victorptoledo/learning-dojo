The search box. People type a piece of a place name, in any case, and often misspell it.
Write the two queries and add their indexes to `indexes.sql`.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `title_contains.sql` | `id`, `title` | the listings whose `title` contains `ribeta`, ignoring case (`ILIKE`); by `id` | 50 buffers |
| `did_you_mean.sql` | `id`, `title`, `similarity` | someone typed `Ribetta`: the 15 listings whose title holds a word most like it, by `pg_trgm`'s `word_similarity('Ribetta', title)`, keeping only those the `<%` operator accepts (`'Ribetta' <% title`); most similar first, then by `id`; `similarity` rounded to 3 decimals | 100 buffers |

`indexes.sql` must create the `pg_trgm` extension (`CREATE EXTENSION IF NOT EXISTS pg_trgm`)
before the indexes that use it.

> [!TIP]
> A pattern that starts with `%` defeats a B-tree: there is no prefix to look up. `pg_trgm`
> splits text into trigrams (`ribeta` → `  r`, ` ri`, `rib`, `ibe`, `bet`, `eta`, `ta `) and
> its operator classes index them: `gin_trgm_ops` for GIN, `gist_trgm_ops` for GiST. The
> same index then answers `LIKE`/`ILIKE` with wildcards on both sides, and the similarity
> operators `%`, `<%` and `%>`, because "similar" means "shares most trigrams". Try
> `SELECT show_trgm('Ribetta')` and `word_similarity('Ribetta', 'Cozy loft in Ribeta')`.
> The index only helps when the condition is in the `WHERE`: an `ORDER BY` on the
> similarity alone still computes it for every row.
