# JSONB and Full-Text Search

Two features that let PostgreSQL replace a second datastore: **JSONB** for semi-structured
documents, and **full-text search** for "find the tickets about a broken login". The SQL track's
lesson [JSON in SQL](../../../sql/p3-advanced/05-json/lesson.md) introduced JSON values, `->`,
`->>` and `JSON_TABLE`; this lesson goes into the PostgreSQL side: containment, jsonpath,
updating documents, how to index them, and how to search text by meaning instead of by
`LIKE`.

## JSONB in one table

`jsonb` stores a parsed, binary document: keys are deduplicated and sorted, whitespace is gone,
and it can be indexed. Use it instead of `json` (which keeps the text as it was) unless you must
preserve key order or duplicates.

```sql
CREATE TABLE products (
    id    bigint PRIMARY KEY,
    name  text NOT NULL,
    attrs jsonb NOT NULL DEFAULT '{}'
);
INSERT INTO products VALUES
  (1, 'Trail shoe', '{"brand": "Kestrel", "sizes": [40, 41, 42], "waterproof": true, "specs": {"weight_g": 310}}'),
  (2, 'Road shoe',  '{"brand": "Kestrel", "sizes": [42, 43], "specs": {"weight_g": 240}}');
```

| Operator | Means | Example |
| --- | --- | --- |
| `->` / `->>` | field as jsonb / as text | `attrs -> 'specs' ->> 'weight_g'` |
| `#>` / `#>>` | path as jsonb / as text | `attrs #>> '{specs,weight_g}'` |
| `@>` | **contains** this document | `attrs @> '{"brand": "Kestrel", "waterproof": true}'` |
| `<@` | is contained in | |
| `?` | has this top-level key (or array element string) | `attrs ? 'waterproof'` |
| `?\|` / `?&` | has any / all of these keys | `attrs ?& array['brand', 'sizes']` |
| `@?` / `@@` | jsonpath exists / jsonpath predicate | `attrs @? '$.sizes[*] ? (@ > 42)'` |
| `-` / `#-` | remove a key / a path | `attrs - 'waterproof'` |
| `\|\|` | merge (shallow) | `attrs \|\| '{"color": "red"}'` |

Containment is the workhorse: `attrs @> '{"sizes": [42]}'` is true for both products, because
an array contains another if it has all its elements.

### jsonpath

jsonpath (SQL standard) navigates and filters inside a document:

```sql
SELECT jsonb_path_query(attrs, '$.sizes[*] ? (@ >= 42)') FROM products WHERE id = 1;   -- 42
SELECT name FROM products WHERE attrs @? '$.specs ? (@.weight_g < 300)';               -- Road shoe
SELECT jsonb_path_query_array(attrs, '$.sizes[last]') FROM products;                   -- [42], [43]
SELECT jsonb_path_exists(attrs, '$.** ? (@ == true)') FROM products;                   -- any true, at any depth
```

`$` is the document, `.key` and `[*]` navigate, `? (...)` filters with `@` as the current item,
`.**` goes down every level, and `strict` / `lax` (default) decide whether a missing key is an
error or simply nothing.

### Changing documents

```sql
UPDATE products SET attrs = jsonb_set(attrs, '{specs,weight_g}', '300') WHERE id = 1;
UPDATE products SET attrs = attrs || '{"color": "red"}' WHERE id = 2;
UPDATE products SET attrs = attrs #- '{specs,weight_g}' WHERE id = 2;
SELECT jsonb_build_object('id', id, 'name', name) FROM products;        -- build
SELECT jsonb_agg(attrs ORDER BY id) FROM products;                      -- aggregate
SELECT key, value FROM products, jsonb_each(attrs) WHERE id = 1;        -- explode
```

Every update rewrites the **whole** document (and its TOAST copy when it is large): documents
that are updated field by field, many times a second, belong in columns.

### Indexing JSONB

| Index | Supports | Size |
| --- | --- | --- |
| `CREATE INDEX ON products USING gin (attrs)` (`jsonb_ops`) | `@>`, `?`, `?\|`, `?&`, `@?`, `@@` | larger |
| `CREATE INDEX ON products USING gin (attrs jsonb_path_ops)` | `@>`, `@?`, `@@` only | smaller and faster for containment |
| `CREATE INDEX ON products ((attrs ->> 'brand'))` | `attrs ->> 'brand' = 'Kestrel'` | a B-tree on one field |

The common mistake is indexing with GIN and querying with `->>`: `WHERE attrs ->> 'brand' =
'Kestrel'` cannot use a GIN index; `WHERE attrs @> '{"brand": "Kestrel"}'` can.

### When to use JSONB, and when not

Good: attributes that vary per row (product specs, user preferences), payloads you store and
return as they are (webhook bodies, audit records), and sparse optional fields. Bad: fields you
join on, constrain with foreign keys, or aggregate in every report. A useful pattern is a
**generated column** that pulls a hot field out of the document:

```sql
ALTER TABLE products ADD COLUMN brand text GENERATED ALWAYS AS (attrs ->> 'brand') STORED;
```

## Full-text search

`LIKE '%login%'` matches substrings, ignores word forms ("logins", "logged") and cannot use a
normal index. Full-text search turns text into **lexemes** (normalised words), drops stop words
and ranks results.

```sql
SELECT to_tsvector('english', 'The users could not log in after the update');
--  'could':3 'log':5 'updat':9 'user':2
SELECT to_tsquery('english', 'users & update');                -- 'user' & 'updat'
SELECT to_tsvector('english', 'The users could not log in after the update')
       @@ to_tsquery('english', 'user & updates');              -- true
```

| Function | Turns | Into |
| --- | --- | --- |
| `to_tsvector(config, text)` | a document | a `tsvector`: lexemes with positions |
| `to_tsquery(config, text)` | `'login & (fail \| error) & !test'` | a `tsquery` (strict syntax) |
| `plainto_tsquery(config, text)` | `'login failed'` | all words ANDed |
| `phraseto_tsquery(config, text)` | `'log in'` | words in order (`<->`) |
| `websearch_to_tsquery(config, text)` | `'"log in" -password or email'` | what users type in a search box; never raises a syntax error |

The **configuration** (`english`, `portuguese`, `simple`, ...) chooses the parser, the stop
words and the stemmer. `simple` only lowercases: use it for codes and names. Always pass the
configuration explicitly; the default comes from `default_text_search_config`.

### Weights, ranking and highlighting

```sql
SELECT id, title,
       ts_rank(doc, q) AS rank,
       ts_headline('english', body, q, 'MaxFragments=1, MaxWords=12') AS snippet
FROM tickets,
     websearch_to_tsquery('english', 'login error') AS q
WHERE doc @@ q
ORDER BY rank DESC
LIMIT 20;
```

`setweight(to_tsvector(...), 'A')` marks lexemes from the title as more important than those of
the body (`'B'`, `'C'`, `'D'`), and `ts_rank` uses the weights. `ts_headline` is slow (it
re-parses the document): call it only for the rows you show.

### Storing and indexing the vector

Computing `to_tsvector` for every row of every search is slow. Store it in a generated column
and index it with GIN:

```sql
ALTER TABLE tickets ADD COLUMN doc tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('english', coalesce(body, '')), 'B')
) STORED;
CREATE INDEX tickets_doc ON tickets USING gin (doc);
```

The expression must name the configuration (`'english'`): the one-argument `to_tsvector(text)`
depends on a setting, so it is not immutable and cannot be used in a generated column or an
index.

### Fuzzy matching with pg_trgm

Full-text search matches words, not typos or fragments. The `pg_trgm` extension splits text into
three-letter **trigrams** and measures similarity, and its GIN or GiST indexes make
`LIKE '%fragment%'`, `ILIKE` and similarity searches fast:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX customers_name_trgm ON customers USING gin (name gin_trgm_ops);
SELECT name, similarity(name, 'Jonh Smiht') FROM customers WHERE name % 'Jonh Smiht'
ORDER BY similarity(name, 'Jonh Smiht') DESC LIMIT 5;     -- % : similar enough (pg_trgm.similarity_threshold)
SELECT * FROM customers WHERE name ILIKE '%smit%';          -- uses the trigram index
```

Full-text search answers "documents about X"; trigrams answer "names that look like X". When you
need relevance tuning, facets, typo tolerance and many languages at scale, a search engine
(OpenSearch, Elasticsearch, Meilisearch) is the next step; until then, PostgreSQL is usually
enough.

## Summary

- Use `jsonb`; query with `@>`, `?` and jsonpath (`@?`, `jsonb_path_query`); update with
  `jsonb_set`, `||` and `#-`.
- GIN indexes serve containment and key operators (`jsonb_path_ops` for `@>` only, smaller); a
  B-tree on `(attrs ->> 'field')` serves equality on one field.
- Keep joinable, constrained and frequently updated fields in columns; pull hot fields out with
  generated columns.
- Full-text search: `to_tsvector` and `websearch_to_tsquery` with an explicit configuration,
  `@@` to match, `setweight` and `ts_rank` to rank, a stored `tsvector` with a GIN index.
- `pg_trgm` covers typos, fragments and fast `ILIKE`.

## Further reading

- PostgreSQL 17, "JSON Types" (with indexing): <https://www.postgresql.org/docs/17/datatype-json.html>
- PostgreSQL 17, "JSON Functions and Operators" (with jsonpath): <https://www.postgresql.org/docs/17/functions-json.html>
- PostgreSQL 17, "Full Text Search": <https://www.postgresql.org/docs/17/textsearch.html>
- PostgreSQL 17, "Controlling Text Search" (parsing, ranking, headlines): <https://www.postgresql.org/docs/17/textsearch-controls.html>
- PostgreSQL 17, "Preferred Index Types for Text Search": <https://www.postgresql.org/docs/17/textsearch-indexes.html>
- PostgreSQL 17, "pg_trgm": <https://www.postgresql.org/docs/17/pgtrgm.html>
