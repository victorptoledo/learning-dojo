# JSON in SQL

Some data has no fixed shape: webhook payloads, user preferences, product attributes that
differ per category. Every major engine can now store JSON and query inside it with SQL.
PostgreSQL's `jsonb` is the most capable, and SQL:2016 standardised the core.

## json versus jsonb

| Type | Stores | Use |
| --- | --- | --- |
| `json` | the text as given (spaces, key order, duplicate keys) | when you must keep the exact text |
| `jsonb` | a parsed binary form; keys sorted, duplicates removed | almost always: faster to query, indexable |

```sql
CREATE TABLE events (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    received_at timestamptz NOT NULL DEFAULT now(),
    type        text NOT NULL,
    payload     jsonb NOT NULL
);

INSERT INTO events (type, payload) VALUES
  ('order.paid', '{"order": {"id": 42, "total": 99.5}, "items": [{"sku": "A1", "qty": 2}, {"sku": "B7", "qty": 1}]}');
```

## Reading values

| Operator | Returns | Example | Result |
| --- | --- | --- | --- |
| `->` | jsonb | `payload -> 'order'` | `{"id": 42, "total": 99.5}` |
| `->>` | text | `payload -> 'order' ->> 'id'` | `'42'` |
| `#>` / `#>>` | jsonb / text at a path | `payload #>> '{order,total}'` | `'99.5'` |
| `-> n` | array element (from 0) | `payload -> 'items' -> 0 ->> 'sku'` | `'A1'` |
| subscript (PostgreSQL 14+) | jsonb | `payload['order']['id']` | `42` |

`->>` returns **text**: cast before comparing or computing: `(payload -> 'order' ->> 'total')::numeric > 50`.
A missing key gives NULL, not an error.

The standard SQL/JSON functions work too (`IS JSON` since PostgreSQL 16; `JSON_VALUE`,
`JSON_QUERY`, `JSON_EXISTS` and `JSON_TABLE` since 17):

```sql
SELECT JSON_VALUE(payload, '$.order.total' RETURNING numeric) FROM events;
SELECT JSON_QUERY(payload, '$.items') FROM events;
SELECT * FROM events WHERE JSON_EXISTS(payload, '$.items[*] ? (@.qty > 1)');
SELECT payload IS JSON OBJECT FROM events;
```

## Querying documents

```sql
-- containment: does the document contain this fragment? (indexable with GIN)
SELECT id FROM events WHERE payload @> '{"order": {"id": 42}}';

-- key exists
SELECT id FROM events WHERE payload ? 'coupon';

-- JSON path with a filter
SELECT id FROM events WHERE payload @? '$.items[*] ? (@.sku == "A1")';
```

## Turning arrays into rows

To aggregate inside documents, unnest them into rows:

```sql
SELECT item ->> 'sku' AS sku, sum((item ->> 'qty')::int) AS units
FROM events e
CROSS JOIN LATERAL jsonb_array_elements(e.payload -> 'items') AS item
WHERE e.type = 'order.paid'
GROUP BY 1;
```

`JSON_TABLE` (standard; PostgreSQL 17, MySQL 8, Oracle) does it declaratively, with types:

```sql
SELECT e.id, jt.sku, jt.qty
FROM events e,
     JSON_TABLE(e.payload, '$.items[*]'
                COLUMNS (sku text PATH '$.sku', qty integer PATH '$.qty')) AS jt;
```

Also `jsonb_each` (key/value rows), `jsonb_object_keys`, `jsonb_array_length`.

## Building JSON from rows

APIs often want nested JSON. Build it in the database in one query:

```sql
SELECT json_build_object(
           'customer', c.name,
           'orders', coalesce(json_agg(json_build_object('id', o.id, 'total', o.total)
                                       ORDER BY o.id) FILTER (WHERE o.id IS NOT NULL), '[]')
       ) AS doc
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name;
```

`to_jsonb(row)` turns a whole row into an object; `jsonb_agg` collects values into an array;
`jsonb_set(doc, '{order,status}', '"paid"')` changes one value; `doc || '{"a": 1}'` merges;
`doc - 'key'` removes a key.

## Indexing

```sql
CREATE INDEX events_payload ON events USING gin (payload);            -- @>, ?, @? on any key
CREATE INDEX events_payload_path ON events USING gin (payload jsonb_path_ops);   -- smaller, @> and @? only
CREATE INDEX events_order_id ON events (((payload -> 'order' ->> 'id')::bigint)); -- one hot field, B-tree
```

The planner has poor statistics on values inside JSON, so estimates for JSON conditions are
often wrong.

## Columns or JSON?

JSON is not a way to skip schema design. Promote a field to a real column when you **filter,
join or sort on it**, when it needs a **constraint** (NOT NULL, a foreign key, a CHECK), or when
it is present in every row. Keep JSON for what is truly variable or what you store as received
(raw payloads, audit snapshots, rarely read settings). A common, healthy shape: typed columns
for the core, and one `jsonb` column for the rest. A generated column can expose a JSON field as
a column: `ADD COLUMN order_id bigint GENERATED ALWAYS AS ((payload -> 'order' ->> 'id')::bigint) STORED`.

## Other engines

| Engine | Type | Read a value | Unnest |
| --- | --- | --- | --- |
| PostgreSQL | `jsonb`, `json` | `->`, `->>`, `JSON_VALUE` | `jsonb_array_elements`, `JSON_TABLE` (17) |
| MySQL | `JSON` (binary) | `->`, `->>` (`col->>'$.order.id'`), `JSON_EXTRACT` | `JSON_TABLE` |
| SQL Server | text in `nvarchar` (a native `json` type in 2025) | `JSON_VALUE`, `JSON_QUERY` | `OPENJSON` |
| SQLite | text | `->`, `->>`, `json_extract` | `json_each` |

## Summary

- Use jsonb; `->` returns JSON, `->>` returns text, `#>>` follows a path. Cast before comparing.
- Containment `@>`, key test `?` and JSON path `@?` query documents; GIN indexes them.
- Unnest arrays with jsonb_array_elements or JSON_TABLE; build JSON with json_build_object and json_agg.
- Promote fields you filter, join, sort or constrain to real columns.

## Further reading

- PostgreSQL 17, "JSON Types": <https://www.postgresql.org/docs/17/datatype-json.html>
- PostgreSQL 17, "JSON Functions and Operators" (SQL/JSON, JSON_TABLE, JSON path): <https://www.postgresql.org/docs/17/functions-json.html>
- Modern SQL, which engines support IS JSON: <https://modern-sql.com/caniuse/is-json>
- SQL Server, "JSON data in SQL Server": <https://learn.microsoft.com/en-us/sql/relational-databases/json/json-data-sql-server>
- SQLite, "JSON Functions and Operators": <https://www.sqlite.org/json1.html>
