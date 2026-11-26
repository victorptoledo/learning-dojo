# Basket Quote

A shop compares prices from several suppliers before it fills a basket. Write `quotes.py`:
a `Money` value type, then an async `quote()` that asks every supplier in batches, at the
same time but within limits, and keeps the cheapest price for each product. `CurrencyMismatch`,
the `PriceSource` protocol, `Line` and `Quote` are already written.

## `Money`: a value type built from special methods

`Money(cents, currency="EUR")` is a frozen dataclass, so `==` and `hash` already compare
both fields. Add:

| Operation | Result |
| --- | --- |
| `a + b` | A new `Money` with the sum. Different currencies raise `CurrencyMismatch` |
| `sum([a, b, c])` | Works, although `sum` starts from the int `0` |
| `a * 3` and `3 * a` | Multiplies by an `int`. A `float`, a `bool` or anything else is a `TypeError` |
| `a < b` (and `<=`, `>`, `>=`) | Compares the cents. Different currencies raise `CurrencyMismatch` |
| `f"{a}"`, `str(a)` | `"1234.56 EUR"`: two decimals, then the currency |
| `f"{a:,.1f}"` | The format spec applies to the amount: `"1,234.6 EUR"` |

An operand of an unsupported type (`a + 1`, `"1" + a`, `a < 5`) must end in a `TypeError`.
Do not raise it yourself: return `NotImplemented` and let Python try the other operand.

## `quote()`: batched, concurrent, bounded

```python
result = await quote({"milk": 2, "bread": 1}, [corner_shop, market],
                     currency="EUR", batch_size=50, limit=4, timeout=1.0)
```

A source is any object with a `name` and `async def prices(skus) -> Mapping[str, Money]`
(the `PriceSource` protocol; there is no base class to inherit).

| Rule | |
| --- | --- |
| Checks first | A quantity that is not an `int` of at least 1 (`0`, `-1`, `1.5`, `True`), a `batch_size` or a `limit` below 1 raise `ValueError` before any call |
| Batches | Sort the SKUs and split them into batches of `batch_size`. Each source is asked **once per batch**, never once per SKU |
| Concurrency | All the calls run concurrently, but never more than `limit` at the same time, across all sources |
| Timeout | Each call has `timeout` seconds. Waiting for a free slot does **not** count |
| Failures | A call that times out or raises an `Exception` puts the source's name in `failed`, and the prices from that call are lost; the other calls go on. Cancelling `quote()` itself must cancel the calls in flight and propagate |
| What counts | Only prices for SKUs that call asked for, and only in `currency` |
| Choice | Each SKU takes the cheapest price; on a tie, the source that comes first in `sources` |
| Result | `Quote(total, lines, missing, failed)`: `lines` in SKU order, `total` the sum of the subtotals (`Money(0, currency)` when there are none), `missing` the SKUs nobody priced, `failed` the names of the sources that failed, sorted and without repeats |

A basket of 20,000 SKUs from two sources must be quoted in well under a second and a half,
so watch for loops that scan a list inside another loop.

Tools: `__add__`, `__radd__`, `__mul__`, `__rmul__`, `__lt__` with `functools.total_ordering`,
`__format__`, `NotImplemented`, `asyncio.TaskGroup`, `asyncio.Semaphore`, `asyncio.timeout`,
sets and dicts for lookups.

Run the visible tests with `uv run pytest`. There are 19 tests: 1 in `tests/test_visible.py`
and 18 hidden ones, plus ruff on `quotes.py`.
