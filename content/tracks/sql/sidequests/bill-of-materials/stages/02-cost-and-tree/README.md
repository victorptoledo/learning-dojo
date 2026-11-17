| File | Columns | Rows and order |
|---|---|---|
| `rollup.sql` | `sku`, `cost` | every **assembly** (bikes included) with the cost of the components below it, at any depth: quantity × `unit_cost`, quantities multiplied down the tree; an assembly with nothing below it costs `0`; most expensive first, equal costs by `sku` |
| `tree.sql` | `depth`, `line` | the tree of `BK-ROAD`, one row per use of a part, depth first |

`tree.sql` prints what a person reads in a parts catalogue. The bike is the first row, at
depth 0; each part comes right after its parent, one level deeper, and the children of a part
are sorted by `sku`. `line` is two spaces per level, the `sku`, `' x'` and the quantity used
in its parent (the bike is `x1`):

```text
 depth | line
-------+-------------------
     0 | BK-ROAD x1
     1 |   BAR-DROP x1
     1 |   BRK-SET x2
     2 |     CABLE x1
     2 |     CALIPER x1
     2 |     PAD x2
     1 |   DT-ROAD x1
   ...
```

A part used in two places (the bearing) appears twice, once under each parent.

> [!TIP]
> For the costs, start the recursion from **every** assembly at once and carry the root along:
> `(root_id, part_id, quantity)`. For the tree, an array of the skus on the way down
> (`path || sku`) is a sort key that puts every row right after its parent.
