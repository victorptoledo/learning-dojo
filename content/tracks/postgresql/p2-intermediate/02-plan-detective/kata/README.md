# Plan Detective

Three cases about **row estimates**, the root of most bad plans. `fixture/01_shop.sql` creates:

```text
addresses      (id, customer_id, city, country)     30,000 rows, 30 cities in 5 countries
orders         (id, customer_id, status, total)     30,000 rows
captured_plans (id, query, plan jsonb)              plans logged by auto_explain as JSON
```

Every table has at most 30,000 rows, so `ANALYZE` reads all of them and the statistics are exact.

| File | Kind | Task |
| --- | --- | --- |
| `statistics.sql` | statements | `WHERE city = 'Porto' AND country = 'PT'` returns 1,000 rows but is estimated at 200. Make the planner estimate city-and-country filters correctly, **both** for real pairs (about 1,000 rows) and for impossible ones such as Porto in `ES` (almost 0), without adding an index or changing data. |
| `common_status.sql` | one query | Read the planner's own statistics (not the table): the most common values of `public.orders.status` and their frequencies, as `status` and `frequency` (rounded to 3 decimals), most frequent first. |
| `worst_estimate.sql` | one query | For each captured plan, the node whose estimate was furthest from reality: `id`, `node_type`, `relation_name` (NULL when the node has none), `plan_rows`, `actual_rows`. "Furthest" is the ratio of the larger to the smaller of the two numbers, in either direction, treating 0 as 1. |

About the plans: `plan` holds the JSON that `EXPLAIN (ANALYZE, FORMAT JSON)` prints, trimmed to a
few fields. Each node is an object with `"Node Type"`, `"Plan Rows"`, `"Actual Rows"` (both per
loop), maybe `"Relation Name"`, and its children in `"Plans"`, nested to any depth. One
expression returns every node of a plan, whatever its depth (you will meet jsonpath properly in
the JSONB lesson):

```sql
SELECT node ->> 'Node Type'
FROM captured_plans, jsonb_path_query(plan, 'strict $.** ? (exists (@."Node Type"))') AS node;
```

Try it with `docker run -it --rm --network none -v "${PWD}:/work" dojo-sql play`, then
`EXPLAIN SELECT * FROM addresses WHERE city = 'Porto' AND country = 'PT';` before and after your
`statistics.sql`.

There are 11 tests: 1 in `tests/visible.sql` and 10 hidden ones. The hidden tests also change
`orders` and run `ANALYZE`, create another table called `orders` in another schema, and add
plans with deeper trees.
