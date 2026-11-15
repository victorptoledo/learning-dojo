# Performance and Profiling

Python is slower than compiled languages for raw computation, and most of the time it does not
matter: programs spend their time waiting on databases and networks, or inside C code (NumPy,
the `dict` implementation, `re`). When it does matter, the rule is always the same: **measure,
find the hot spot, fix that, measure again.**

## Measuring: benchmarks

```text
uv run python -m timeit -s "xs = list(range(10_000))" "sum(xs)"
uv run python -m timeit -s "xs = list(range(10_000)); s = set(xs)" "9_999 in s"
```

`timeit` runs a statement many times and reports the best average, which hides noise from the
rest of the machine. In code, use `time.perf_counter()` (monotonic, high resolution), never
`time.time()`. For serious comparisons, `pyperf` runs calibrated, repeated benchmarks.

## Finding the hot spot: profilers

**Deterministic** profiling records every function call:

```text
uv run python -m cProfile -s cumtime report.py | head -30
uv run python -m cProfile -o report.prof report.py     # save, then explore:
uvx snakeviz report.prof                                # a browser view of the call tree
```

| Column | Meaning |
| --- | --- |
| `ncalls` | How many times the function was called (`120000` is often the clue) |
| `tottime` | Time inside the function itself |
| `cumtime` | Time inside it **and everything it called**: sort by this first |

cProfile slows the program down and distorts very small functions. **Sampling** profilers look
at the stack many times per second instead, with little overhead, and can attach to a running
process, even in production:

```text
py-spy top --pid 12345                    # a live "top" of Python functions
py-spy record -o profile.svg -- python report.py   # a flame graph
```

Since **3.15** the standard library has its own sampling profiler, **Tachyon**
(`profiling.sampling`, PEP 799), so you no longer need to install one:

```text
python -m profiling.sampling run script.py                       # profile from the start
python -m profiling.sampling run --flamegraph -o profile.html script.py
python -m profiling.sampling attach 12345                        # a running process
python -m profiling.sampling run --live script.py                # a live, top-like view
python -m profiling.sampling dump 12345                          # every thread's stack, once
```

It can measure wall-clock time, CPU time or GIL-holding time (`--mode`), and understands
asyncio tasks (`--async-aware`). The deterministic profiler moved to `profiling.tracing`;
`cProfile` remains as an alias.

Other useful tools: **Scalene** (CPU, memory and time spent in native code, per line),
**line_profiler** (`@profile` line by line) and **memray** (allocation tracking).

### Memory

```python
import tracemalloc

tracemalloc.start()
build_report()
snapshot = tracemalloc.take_snapshot()
for stat in snapshot.statistics("lineno")[:5]:
    print(stat)          # the lines that allocated the most memory
```

`sys.getsizeof(x)` gives the size of one object, not of what it references.

## The usual wins

Measured over many code bases, these fix most slow Python, in this order:

| Problem | Fix |
| --- | --- |
| `x in some_list` inside a loop | Build a `set` (or `dict`) once: O(1) instead of O(n) per check |
| Nested loops matching two lists | Index one side in a dict, then a single loop (O(n + m) instead of O(n·m)) |
| N+1 queries or HTTP calls in a loop | One batched query (`WHERE id IN (...)`), a join, or concurrent requests |
| `s += piece` for many pieces | `parts.append(piece)` then `"".join(parts)` |
| Recomputing the same pure result | `functools.cache` / `lru_cache`, or compute once outside the loop |
| Python loops over numbers | Built-ins (`sum`, `max`, `sorted`, `any`) run in C; NumPy for arrays |
| Loading everything into memory | Generators and streaming (the generators lesson) |
| Millions of small objects | `@dataclass(slots=True)` or tuples; arrays for numbers |
| Many small writes | Buffer them; batch inserts with `executemany` or `COPY` |

Micro-optimisations (caching an attribute lookup in a local variable, avoiding a function call
in a tight loop) give a few percent; use them only in a loop a profile has proven hot.

```python
# before: O(n·m), and the list is rebuilt for every order
total = sum(o.amount for o in orders if o.customer_id in [c.id for c in vips])

# after: O(n + m)
vip_ids = {c.id for c in vips}
total = sum(o.amount for o in orders if o.customer_id in vip_ids)
```

## A faster interpreter

CPython itself keeps getting faster. **3.11** added the *specialising adaptive interpreter*
(PEP 659): bytecode that runs often is rewritten into versions specialised for the types it
actually sees (an `int + int`, a dict attribute lookup), which made typical code 10–60%
faster. **3.13** added an experimental **JIT compiler** (PEP 744, "copy-and-patch"), off by
default and only in builds configured for it; in 3.13 it laid the groundwork rather than giving big
gains. **3.15** upgraded it substantially (more supported bytecodes, register allocation,
constant folding): pyperformance reports about 7–8% over the standard interpreter on x86-64
Linux on average, with results ranging from slower to twice as fast. It is still
experimental, so measure your own workload. Upgrading Python is often the cheapest
optimisation you can make.

**PyPy**, an alternative implementation with a tracing JIT, can run long-running pure-Python
code several times faster, at the cost of compatibility with some C extensions.

## Going native

When a hot loop is pure computation and the algorithm is already right, move it out of the
interpreter:

| Option | When |
| --- | --- |
| **NumPy** (and pandas, Polars) | Numeric arrays: one vectorised call replaces a Python loop over millions of items |
| **Cython** | Typed, Python-like code compiled to C; incremental, good for existing code |
| **Rust with PyO3 and maturin** | New, safe, fast extension modules (Polars, pydantic-core and ruff are written this way) |
| C extensions / `ctypes` / `cffi` | Wrapping existing C libraries |
| Processes or free-threaded threads | Using more cores for work that parallelises (the asyncio and GIL lesson) |

```python
import numpy as np

prices = np.array(price_list)
quantities = np.array(quantity_list)
revenue = (prices * quantities).sum()     # one call into C, no Python loop
```

## Keeping it fast

A fix nobody measures will regress. Keep a benchmark for the critical paths (pytest-benchmark,
or a timed test with a generous limit), watch latency percentiles in production metrics, and
profile again after big changes.

## Summary

- Measure first: `timeit` and `perf_counter` for benchmarks, cProfile sorted by `cumtime`,
  py-spy for running processes, `tracemalloc` for memory.
- Most wins are algorithmic: sets and dicts for lookups, no N+1 calls, `"".join`, caching,
  streaming.
- Newer CPython versions are faster (specialising interpreter, experimental JIT); PyPy is an
  option for pure Python.
- For hot numeric loops go native: NumPy, Cython or Rust with PyO3.

## Further reading

- The Python Profilers (`cProfile`, `pstats`): <https://docs.python.org/3/library/profile.html>
- `timeit`: <https://docs.python.org/3/library/timeit.html>
- `tracemalloc`: <https://docs.python.org/3/library/tracemalloc.html>
- `profiling.sampling`, the Tachyon sampling profiler: <https://docs.python.org/3/library/profiling.sampling.html>
- PEP 659, specializing adaptive interpreter: <https://peps.python.org/pep-0659/>
- PEP 744, JIT compilation: <https://peps.python.org/pep-0744/>
- py-spy, a sampling profiler: <https://github.com/benfred/py-spy>
- roadmap.sh, Python topic "Data Structures and Algorithms": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/data-structures--algorithms@VJSIbYJcy2MC6MOFBrqXi.md>
