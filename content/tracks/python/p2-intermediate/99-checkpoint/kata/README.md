# Resilience Kit

Write three small tools in `resilience.py` that services use around unreliable calls. Time
and sleeping are **injected** (`clock`, `sleep`), so the tests run instantly with fakes. The
exceptions are already written: `ResilienceError` is the base class, `RetryExhausted(name,
attempts)` keeps `attempts`, and `CircuitOpen` means a call was not attempted.

## `retry`: a decorator with arguments

```python
@retry(times=3, on=ConnectionError, delay=0.5, backoff=2.0)
def fetch_rates(): ...
```

| Rule | |
| --- | --- |
| Attempts | Calls the function up to `times` times and returns the first result that does not raise |
| What is retried | Only exceptions matching `on` (a class or a tuple of classes; default `Exception`). Any other exception propagates at once, without retrying |
| Waiting | Between attempts, calls `sleep(delay * backoff ** (attempt - 1))`: with `delay=0.5, backoff=3` the pauses are 0.5, 1.5, 4.5. It never sleeps after the last attempt |
| Giving up | After the last failed attempt, raises `RetryExhausted(function name, times)` **chained** to the last error, so `__cause__` is that error |
| Metadata | The decorated function keeps its `__name__` and `__doc__`, and `__wrapped__` is the original |
| Bad arguments | `retry(times=0)` (any `times` below 1) raises `ValueError` when `retry` is called |

## `CircuitBreaker`: a context manager and a decorator

`CircuitBreaker(threshold=3, reset_after=30.0, *, clock=time.monotonic)` protects calls to a
dependency. `threshold` below 1 raises `ValueError`. Its `state` property is one of:

| State | Meaning | `with breaker:` |
| --- | --- | --- |
| `"closed"` | Normal | Runs the block. A block that raises an `Exception` counts one failure; one that succeeds resets the count to 0. `threshold` consecutive failures open the circuit |
| `"open"` | Failing, less than `reset_after` seconds (by `clock()`) since it opened | Raises `CircuitOpen` **without** running the block |
| `"half-open"` | At least `reset_after` seconds since it opened | Runs the block as a trial: success closes the circuit (count 0); failure opens it again and restarts the timer |

The breaker never suppresses the block's exception: it records it and lets it propagate.
Used as a decorator (`@breaker` above a function), every call runs inside `with breaker:`,
and the function keeps its name and docstring.

## `timer`: a generator-based context manager

```python
with timer() as timing:      # clock defaults to time.perf_counter
    work()
print(timing.elapsed)        # seconds, as a float
```

`timing` is a `Timing` (already written) whose `elapsed` is `None` inside the block and is set
when the block ends, **also when it raises** (the exception still propagates).

Tools: `functools.wraps`, a decorator factory, `raise ... from error`, `__enter__` and
`__exit__`, `__call__`, `contextlib.contextmanager` with `try`/`finally`.

Run the visible tests with `uv run pytest`. There are 16 tests: 1 in
`tests/test_visible.py` and 15 hidden ones, plus ruff on `resilience.py`.
