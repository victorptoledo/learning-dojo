# Cell Router

A service runs as **cells**: full, independent copies, each serving part of the tenants. You
write `CellRouter`, the data plane in front of them. It sends each tenant to a **shuffle shard**
of a few cells, so a poison tenant can only take down its own cells. It keeps working with the
last good map when the **control plane** is down or pushes a bad map. When cells fail, it
degrades on purpose instead of falling over.

`Cell`, `ControlPlane`, `CellMap`, `Request`, `Response`, `Priority`, `Outcome`, `Hashing` and
the two exceptions are given: do not change them.

```java
var router = new CellRouter(new CellMap(1, List.of("c1", "c2", "c3", "c4"), 2), controlPlane, cells);
router.shardOf("acme");                                       // e.g. [c3, c1]
router.route(new Request("acme", Priority.NORMAL, "hello"));  // SERVED by c3: "c3:hello"
```

## The map

A `CellMap(version, cells, shardSize)` is **valid** when its version is at least 1, it lists at
least one cell, no cell twice and only cells the router knows (the keys of the `cells` map it
was built with), and `1 ≤ shardSize ≤ number of cells`.

- `CellRouter(initial, controlPlane, cells)`: an invalid initial map throws
  `IllegalArgumentException`. The router starts from this map and **does not call the control
  plane** to start.
- `map()`: the map in use.
- `refresh()` asks `controlPlane.latest()` once and returns `true` only if it installed a new map:
  - `ControlPlaneDownException`: keep the map and return `false`. The data plane runs on the
    last good map (**static stability**).
  - A version **not higher** than the current one: ignore it (a replay or a lagging control
    plane replica), return `false`. Versions work like fencing tokens.
  - A newer but **invalid** map: keep the current one, count it in `rejectedMaps()` and return
    `false`. A bad push must not take the data plane down.
  - Otherwise install it. Cells that left the map stop being ejected.

## Shuffle shards

`shardOf(tenant)` is the `shardSize` cells of the map with the lowest score
`Hashing.hash64(tenant + "|" + cell)` (as a signed `long`, ties by cell name), **lowest first**.
That is also the order in which they are tried. This is rendezvous hashing: when a cell joins,
a tenant's shard either stays the same or swaps one cell for the new one.

## Health

`probe()` calls `ping()` on every cell **of the map**. A cell is unhealthy when it answers
`false` or throws `CellDownException`.

- If **more than half** of the map's cells are unhealthy, the router stops trusting the checks:
  it ejects **no** cell and `panicking()` is `true`. A broken health checker, or a shared
  dependency, must not make the router send all traffic to a few cells.
- Otherwise it ejects exactly the unhealthy cells (replacing the previous ejections) and
  `panicking()` is `false`. `ejected()` lists them sorted by name.

## `route(request)`

1. The **available** cells are the tenant's shard minus the ejected ones, in shard order.
2. **Shed** (return `Response(SHED, null, null)` without calling any cell):
   - while panicking, everything except `CRITICAL`;
   - otherwise, `BATCH` work when the shard is short of cells (some of its cells are ejected).
3. Try the available cells in order. The first one that answers gives
   `Response(SERVED, cell, body)`. A cell that throws `CellDownException` is skipped, and the
   router tries the next one.
4. If none answered: `Response(UNAVAILABLE, null, null)`.

A request is **never** sent to a cell outside its tenant's shard, even when the whole shard is
down. Spilling it over would spread a poison tenant to every cell. Routing and probing never
call the control plane.

## What the grading tests check

Map validation and refresh (down, stale, invalid, newer), shards against the rule above and how
few tenants move when a cell joins, failover inside the shard, the blast radius of a poison
tenant over 1,000 others, ejection, panic mode, shedding by priority, that the data plane never
needs the control plane, and that the router keeps no static state.

> Tip: keep `map`, a `Set<String> ejected` (a `TreeSet` sorts it for you), `boolean panicking`
> and a counter. Sorting `map.cells()` with
> `Comparator.comparingLong((String c) -> Hashing.hash64(tenant + "|" + c)).thenComparing(Comparator.naturalOrder())`
> and taking `limit(shardSize)` gives the shard.
