# Catalog Tour

`fixture/01_shop.sql` creates a small shop in two schemas, `shop` and `audit`:

```text
shop.customers, shop.products, shop.orders, shop.order_items (primary key order_id, line_no),
shop.shipments (foreign key order_id, line_no), shop.reviews (long texts), audit.log
```

You will not read those tables' rows. Every answer comes from the **system catalogs**
(`pg_class`, `pg_namespace`, `pg_attribute`, `pg_constraint`, `pg_index`, `pg_settings`) and the
size functions. Write one query in each file:

| File | Returns | Order |
| --- | --- | --- |
| `largest_tables.sql` | every ordinary or partitioned table of the schemas `shop` and `audit`: `schema_name`, `table_name`, `total_bytes` (the table with its TOAST data and indexes) and `index_bytes` (its indexes only) | `total_bytes` descending, then schema, then table name |
| `columns.sql` | the columns of `shop.orders` as `\d` shows them: `column_name`, `data_type` (as `format_type` writes it, for example `numeric(12,2)`) and `not_null` (a boolean). No system columns (`xmin`, `ctid` ...) and no dropped ones | the order of the columns in the table |
| `unindexed_fks.sql` | the foreign keys of the schema `shop` that **no index covers**: `table_name` (as `regclass` prints it, such as `shop.order_items`), `constraint_name` and `columns` (the key's columns joined with `, ` in the key's order) | any |
| `changed_settings.sql` | every setting whose value does not come from the built-in default, that is whose `source` is neither `default` nor `override`: `name`, `setting`, `unit`, `source` | by `name` |

An index **covers** a foreign key when its first columns are exactly the key's columns, in any
order. An index on `(stars, product_id)` does not cover a key on `product_id`, because a lookup
by `product_id` alone cannot use it; an index on `(line_no, order_id)` does cover a key on
`(order_id, line_no)`. Foreign keys without an index make every `DELETE` of a parent row scan
the child table, and they are the most common missing index in real databases.

Two catalog details you will need:

- `pg_attribute` keeps dropped columns (`attisdropped`) and lists system columns with
  `attnum < 0`.
- `pg_index.indkey` is an `int2vector` that starts at index 0; `indkey::text` gives `'3 2'`, and
  `string_to_array(indkey::text, ' ')::smallint[]` turns it into an ordinary array starting at 1,
  like `pg_constraint.conkey`.

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`, then `\d shop.*`
and `\set ECHO_HIDDEN on` to see the catalog queries psql itself runs.

There are 12 tests: 1 in `tests/visible.sql` and 11 hidden ones. Some hidden tests change the
schema first (they drop a column, add an index, create a partitioned table or another `orders`
table in `public`), so a query that only matches the fixture will not pass.
