# Metrics Core

Write the library declared in `metrics.hpp` and `metrics_c.hpp`: the two `constexpr`
functions in `metrics.hpp`, the rest in `metrics.cpp`.

## At compile time

| Function | Does |
| --- | --- |
| `valid_name(name)` | 1 to 64 characters: a lower-case letter, then lower-case letters, digits and `_`. |
| `exponential_bounds<N>(start, factor)` | `start, start * factor, ...`, N values. `std::invalid_argument` unless `start > 0` and `factor > 1` (also for NaN). Both must work in a `constexpr` variable. |

## Histogram and Registry

| Member | Does |
| --- | --- |
| `Histogram(bounds)` | Copies the bounds. `std::invalid_argument` if there are none, or they are not finite and strictly increasing. All its memory is allocated here. |
| `record(value)` | `noexcept`, **no allocation**: counts the value in the first bucket whose bound is `>=` the value (one extra bucket for values above every bound) and adds it to the sum. NaN is ignored. |
| `quantile(q)` | The bound of the bucket holding the `ceil(q * count)`-th smallest value (at least the first), `+inf` for the last bucket, 0 when empty; `std::invalid_argument` unless `0 <= q <= 1`. |
| `Registry::define(name, bounds)` | Adds a histogram; `std::invalid_argument` for a bad name, bad bounds or a name already defined (and then adds nothing). |
| `Registry::find(name)` | The histogram or `nullptr`, **with no allocation**, even for names longer than the small-string buffer. |
| `Registry::render()` | Every histogram, sorted by name, in the Prometheus text format with **cumulative** buckets and numbers printed by `{}`: |

```text
latency_ms_bucket{le="1"} 2
latency_ms_bucket{le="2.5"} 3
latency_ms_bucket{le="+Inf"} 5
latency_ms_sum 40.5
latency_ms_count 5
```

## The C interface

Every function returns a status (`DOJO_OK`, `DOJO_UNKNOWN_METRIC`, `DOJO_INVALID_ARGUMENT`,
`DOJO_OUT_OF_MEMORY`) and **no exception ever leaves it**.

| Function | Does |
| --- | --- |
| `dojo_registry_new()` | A new registry, or `NULL` when memory runs out. |
| `dojo_registry_free(r)` | Frees it; `NULL` is fine. |
| `dojo_define(r, name, bounds, count)` | `DOJO_INVALID_ARGUMENT` for null pointers (bounds may be null only when count is 0), bad names or bounds and duplicates; `DOJO_OUT_OF_MEMORY` when an allocation fails. |
| `dojo_record(r, name, value)` | `DOJO_UNKNOWN_METRIC` for an unknown name, `DOJO_INVALID_ARGUMENT` for null pointers or NaN. No allocation. |
| `dojo_render(r, buffer, capacity)` | Like `snprintf`: at most `capacity - 1` characters and a NUL (nothing at all when `capacity` is 0), and returns the length of the whole text. A `NULL` registry writes `""` and returns 0. |

The dojo runs 15 hidden tests and the visible one under AddressSanitizer and UBSan, plus the
tool checks on the three files. The tests replace the global `operator new` to count
allocations and to make one fail on purpose.
