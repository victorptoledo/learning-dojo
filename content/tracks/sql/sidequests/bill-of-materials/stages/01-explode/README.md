Read the fixture first: `SELECT * FROM bom JOIN parts ON parts.id = bom.child_id WHERE
parent_id = 1` lists what a road bike is made of.

| File | Columns | Rows and order |
|---|---|---|
| `explode.sql` | `sku`, `name`, `quantity` | every **component** needed to build one `BK-ROAD`, with the total quantity; by `sku` |
| `where_used.sql` | `sku`, `level` | every part that uses `BEARING`, directly (`level` 1) or through other assemblies (2, 3, ...); a part reached at several levels shows the nearest; by `level`, then `sku` |

Quantities multiply down the tree: a bike has 2 wheels and a wheel has 32 spokes, so the bike
needs 64 spokes. A component used in several branches adds up: the bearings sit in the hubs
**and** in the bottom bracket.

> [!TIP]
> A recursive CTE has an anchor (the direct children of the bike, or the direct parents of
> the bearing) and a recursive part that joins the rows found so far back to `bom`. Carry
> the multiplied quantity, or the level, as a column.
