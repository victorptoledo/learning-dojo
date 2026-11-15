# Log Pipeline

Write a lazy pipeline over log lines in `pipeline.py`. Every function that returns an
iterator must be **lazy**: it reads its input only as results are requested, so it works on a
log of any size, and even on an endless stream (the tests use one).

A log line has four fields separated by whitespace; the message is the rest of the line and
may be empty:

```text
2026-11-14T10:00:07+00:00 ERROR payments card declined
^ time (ISO 8601)         ^ level ^ service ^ message
```

`Entry(time, level, service, message)` is already written: a frozen dataclass whose `time` is
a `datetime`.

| Function | Returns |
| --- | --- |
| `parse(lines)` | An iterator of `Entry`. Skips blank lines, lines starting with `#`, and malformed lines: fewer than three fields, a level not in `LEVELS`, or a time that `datetime.fromisoformat` rejects. Surrounding whitespace (and the newline) is not part of the message |
| `at_least(entries, level)` | An iterator of the entries whose level is `level` or more severe (`LEVELS` is in order). An unknown level raises `ValueError` **when `at_least` is called**, not later |
| `per_minute(entries)` | An iterator of `(minute, count)`, one per **run of consecutive entries** in the same minute, with the minute formatted as `2026-11-14T10:00` |
| `batches(entries, size)` | An iterator of lists of up to `size` entries, in order. A `size` below 1 raises `ValueError` **when `batches` is called** |
| `busiest_service(entries)` | The service with the most entries (ties: the alphabetically first), or `None` when there are none. This one consumes everything |

For `sample.log` (in the kata folder):

```python
with open("sample.log", encoding="utf-8") as lines:
    entries = list(parse(lines))     # 6 entries
list(per_minute(entries))            # [('2026-11-14T10:00', 3), ('2026-11-14T10:01', 2), ('2026-11-14T10:03', 1)]
[e.level for e in at_least(entries, "WARNING")]   # ['ERROR', 'WARNING', 'CRITICAL']
busiest_service(entries)             # 'payments'
```

Tools: `yield`, generator expressions, `str.split(maxsplit=3)`, `datetime.fromisoformat`,
`datetime.strftime`, `itertools.groupby`, `itertools.batched`.

Run the visible tests with `uv run pytest`. There are 14 tests: 1 in
`tests/test_visible.py` and 13 hidden ones, plus ruff on `pipeline.py`.
