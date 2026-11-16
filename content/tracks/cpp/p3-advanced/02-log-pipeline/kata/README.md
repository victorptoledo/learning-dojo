# Log Pipeline

Write the functions declared in `logs.hpp`, in `logs.cpp`. A log line looks like this:

```text
TIMESTAMP LEVEL SERVICE LATENCYms MESSAGE...
1700000000 ERROR billing 250ms card declined
```

| Function | Returns |
| --- | --- |
| `lines(text)` | A `std::generator<std::string_view>`: each line without `'\n'` (and a trailing `'\r'`), skipping blank lines. Lazy: nothing is split until the loop asks. |
| `parse_entry(line)` | An `Entry`, or `std::nullopt` for a line with missing fields, a timestamp or latency that is not a number, a latency without `ms` or a level other than `DEBUG`, `INFO`, `WARN`, `ERROR`. The message is the rest of the line (it may be empty). |
| `parse_all(text)` | Every well-formed entry, in order. |
| `errors_by_service(entries)` | How many `ERROR` entries each service has. |
| `slowest(entries, n)` | The `n` slowest entries: highest latency first, earlier timestamp first on a tie. |
| `services_at_least(entries, level)` | The services with an entry at `level` or above, each once, sorted. |
| `moving_average(values, window)` | The mean of each run of `window` consecutive values; empty when `window` is 0 or larger than the input. |

Write them as pipelines where it reads well: `std::views::filter`, `transform`, `slide`,
`std::ranges::to<std::vector>()` (and `to<std::set>()`), `std::ranges::partial_sort`.
`std::from_chars` reads numbers without exceptions or locales.

The string views from `lines` point into `text`. That is safe here because `text` outlives
the loop, but keep the rule in mind: a view never owns what it shows.

10 hidden tests and the visible one, under AddressSanitizer, plus the tool checks.
