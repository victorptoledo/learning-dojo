**`convert_totals(orders, rate)`** sums each customer's orders in euros. `rate(currency)` asks
a remote service that takes about 10 ms per call, and the job calls it once per order.

The contract test counts the calls: one call per **currency** per run is the most allowed,
and 2 000 orders in 4 currencies must finish within **one second**.

Two traps: `@functools.cache` on a module-level helper would keep last night's rates forever
(rates change between runs, so the cache must not outlive one call of `convert_totals`), and
the totals must stay exactly the same, cent for cent, so add the amounts in the same order as
before.
