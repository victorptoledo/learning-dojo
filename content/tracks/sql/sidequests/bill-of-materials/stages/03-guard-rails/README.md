The engineers edit `bom` by hand, and one wrong row can make an assembly part of itself. Your
queries from the earlier stages would then recurse until PostgreSQL stops them. Two checks
run before every release of the catalogue:

| File | Columns | Rows and order |
|---|---|---|
| `cycles.sql` | `sku` | every part that is, through one or more `bom` rows, part of itself; by `sku` |
| `bad_parts.sql` | `sku`, `problem` | one row per part and problem below; by `sku`, then `problem` |

The problems, with these exact texts:

| `problem` | When |
|---|---|
| `assembly without parts` | an assembly that is the parent of no `bom` row |
| `assembly with a cost` | an assembly with a `unit_cost` (its cost comes from its parts) |
| `component with parts` | a component that is the parent of a `bom` row |
| `component without cost` | a component whose `unit_cost` is `NULL` |
| `component used nowhere` | a component that is the child of no `bom` row |

The catalogue is clean today, so both queries return no rows on the fixture. The tests break
it first. `cycles.sql` must finish on a catalogue with cycles, in less than a second.

> [!TIP]
> A recursive walk from every part can remember the parts it went through (an array, or
> PostgreSQL's `CYCLE ... SET ... USING ...` clause) and stop at the first repeat. A part is
> in a cycle when its own walk reaches it again.
