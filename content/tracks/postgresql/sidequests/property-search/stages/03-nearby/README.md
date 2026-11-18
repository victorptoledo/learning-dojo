The map view. `location` is a `point` on a 100 × 100 km map, and distances are straight
lines in km (`<->` between two points). Write the two queries and add their indexes to
`indexes.sql`.

| File | Columns | Rows and order | Budget |
|---|---|---|---|
| `nearest.sql` | `id`, `distance` | the 10 listings closest to the point `(50.5, 50.5)`, closest first; `distance` rounded to 3 decimals | 30 buffers, and no `Sort` in the plan |
| `within_radius.sql` | `listings` | one row: how many listings with **3 bedrooms or more** lie within 2 km of `(20, 80)` (on the circle counts as inside) | 200 buffers |

> [!TIP]
> "Closest first" is an `ORDER BY` on a distance that changes with every search, so no B-tree
> can hold it in order. A GiST index on `location` can: written as `ORDER BY location <->
> point '(50.5, 50.5)' LIMIT 10`, the plan becomes an `Index Scan` that returns points nearest
> first and stops after 10 (a "KNN" search), with no `Sort`. Put the distance on the right of
> `<->` exactly like that; an expression around it hides it from the index. For the radius,
> a `circle '((x, y), r)'` and the containment operator `<@` use the same index.
