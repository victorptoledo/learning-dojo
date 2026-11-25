# Final Exam: Six Incidents Before Go-Live

`IngestService` takes order lines (`id;customerId;sku;quantity;priceCents`), parses them,
runs the configured enrichers, looks up the customer's name and stores the order. It works on
a laptop. The production readiness review found **six incidents**. Fix them all, and keep the
public API: the grading tests build the service with their own logger, directory and sink.

Already written and not to change: `Order`, `BadOrderException`, `ConfigException`,
`CustomerDirectory`, `OrderSink`, `Enricher`, `TaxEnricher`, `BulkEnricher`, `Metrics` and
`ShutdownReport`. You change `Config`, `OrderParser`, `Enrichers`, `LruCache` and
`IngestService`.

## Incident 1: the configuration fails late and leaks

`Config.fromEnv(env)` must check **everything at startup** and throw **one**
`ConfigException` listing **every** problem, in this order of keys, each problem starting with
`"KEY: "` and naming the bad value (never the password):

| Key | Rule | Default |
| --- | --- | --- |
| `PORT` | whole number, 1 to 65535 | 8080 |
| `DB_URL` | required, starts with `jdbc:` | none |
| `DB_PASSWORD` | required | none |
| `WORKERS` | whole number, 1 to 64 | 4 |
| `CACHE_SIZE` | whole number, 1 to 1,000,000 | 1000 |
| `ENRICHERS` | comma-separated names, each one known (`tax`, `bulk`) | none |

Values are trimmed; a blank value counts as missing; empty names in `ENRICHERS` are skipped.
`Config.toString()` must not show the password (but keep the other settings readable: it goes
in the startup log).

## Incident 2: the logs

The service logs through the `System.Logger` it is given:

- one `INFO` line at startup (with the masked config) and one when it stops;
- no log line ever contains the password;
- a message for a level that is off is **never built**: use `log(level, () -> ...)`,
  `log(level, "format {0}", params)` or `isLoggable`, never `log(level, "text " + value)`;
- each failure (a rejected line, an order the sink refused) is logged **once**, at `WARNING`,
  **with the exception attached** (`log(level, message, throwable)`), and never with
  `printStackTrace`. A client's bad line is not an `ERROR`.

## Incident 3: the cache that never stops growing

Customer names are cached in a `static HashMap`: it is shared by every service in the JVM,
never shrinks, and is not safe for the worker threads. Write `LruCache<K, V>`: a capacity of
at least 1 (`IllegalArgumentException` otherwise), `get(key, loader)` that returns the cached
value (a hit) or loads, stores and returns it (a miss), evicting the **least recently used**
entry when it is full, and `size()`, `hits()`, `misses()`. It must be safe to share between
threads. The service keeps **its own** cache of `CACHE_SIZE` entries and reports it in
`metrics()`.

## Incident 4: the hot path allocates too much

The profiler shows `OrderParser.parse` at the top of the allocation flame graph: about 2 KB per
line. The budget is **256 bytes per line**, measured over 100,000 calls with
`ThreadMXBean.getCurrentThreadAllocatedBytes()`. The `Order` and its three strings fit in it;
`split`, `Pattern.compile`, a `Matcher`, `String.format` and streams do not. Keep every rule:
five fields; ids not empty and without spaces; a SKU of three capital letters, a dash and four
digits (`ABC-1234`); quantity 1 to 1000; price 0 to 10,000,000 cents. Anything else is a
`BadOrderException`.

## Incident 5: a deploy loses orders

`shutdown(grace)` calls `shutdownNow()` and drops the queued orders. It must:

1. stop taking orders at once (`submit` then returns `false`, without throwing);
2. let the accepted orders finish, waiting up to `grace`;
3. only then interrupt what is still running, and return a `ShutdownReport` where
   `stored + failed + abandoned` equals the accepted orders. An order interrupted by the
   shutdown is **abandoned**, not failed.

`metrics()` counts accepted lines (queued for a worker), rejected lines (bad input), stored
and failed orders.

## Incident 6: the native image cannot find the enrichers

`Enrichers.named` builds class names and loads them with `Class.forName`: the closed world of
a native image does not see them. Replace it with an **explicit registry** of the known names
(`tax` and `bulk`, case-sensitive), applied in the configured order. An unknown name is an
`IllegalArgumentException` (and a problem in `Config.fromEnv`). A test reads your sources:
no `Class.forName`, `newInstance(`, `getDeclaredConstructor`, `getConstructor(`,
`java.lang.reflect` or `ServiceLoader` anywhere under `src/main/java`.

> [!TIP]
> Use the JDK only. Run the visible tests with `mvn test` from the kata folder (the source scan
> reads `src/main/java` from there). Measure before you optimise: the allocation test prints
> how many bytes each line costs.
