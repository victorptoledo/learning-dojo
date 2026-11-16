# Data Quality Checks

Before a batch is published, a pipeline should check it the way dbt tests and Great
Expectations do: declare what must be true, run every check, and stop when an important one
fails. Write that little framework in `checks.py`. A batch is a list of rows, each a `dict`;
a missing key counts as `None`.

## Checks

Each function returns a `Check` (`name`, `severity`, `evaluate`); `evaluate(rows)` returns a
`CheckResult` with `passed`, `failing` (how many rows failed) and `sample` (the indexes of
the first **3** failing rows, in row order).

| Function | Name | A row fails when |
| --- | --- | --- |
| `not_null(column)` | `not_null(column)` | the value is `None` (`0` and `""` are values) |
| `unique(*columns)` | `unique(a, b)` | its key appears in more than one row: **every** such row fails; a key with a `None` is ignored, like SQL's `UNIQUE`; no column at all is a `ValueError` |
| `accepted_values(column, values)` | `accepted_values(column)` | the value is not one of `values` (exact match); `None` is left to `not_null` |
| `in_range(column, low=None, high=None)` | `in_range(column)` | the value is below `low` or above `high` (inclusive; `None` bounds are open); `None` values pass |
| `relationship(column, parents, parent_column)` | `relationship(column -> parent_column)` | the value is not in `parent_column` of any parent row (an orphan); `None` passes. `parents` may be a generator: the check can run many times |

Two checks look at the **whole batch**: their `failing` is 1 or 0 and their `sample` is
empty.

| Function | Name | Fails when |
| --- | --- | --- |
| `row_count(low, high)` | `row_count(low..high)` | the number of rows is outside `low..high` (inclusive) |
| `freshness(column, max_age, now)` | `freshness(column)` | the newest non-`None` value is older than `now - max_age`, or there is none |

Every check takes `severity="error"` (the default) or `"warn"`; anything else is a
`ValueError`.

## Reports

`run_checks(rows, checks)` runs the checks in order and returns a `Report` with the results
in that order. Its `passed` is true when no **error** failed (warnings never block),
`failures` and `warnings` list the failed results of each severity, and `summary()` returns
one line per check and a total:

```text
FAIL unique(order_id): 2 failing, rows 2, 3
WARN in_range(amount): 1 failing, rows 1
FAIL row_count(1..3)
PASS freshness(loaded_at)
1 passed, 2 failed, 1 warnings
```

A failed batch check has no `: ... failing` part. `assert_quality(rows, checks)` is the
**quality gate**: it returns the report when it passed and raises `QualityError(report)`
otherwise. You will use it in the next checkpoint.

Run the visible tests with `uv run pytest`. There are 23 tests: 2 in `tests/test_visible.py`
and 20 hidden ones, plus ruff on `checks.py`.
