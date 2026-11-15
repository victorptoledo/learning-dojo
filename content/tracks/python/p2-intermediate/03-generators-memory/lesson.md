# Iterators, Generators and Lazy Pipelines

`for` works on lists, files, dicts, ranges, database cursors and network streams alike,
because they all speak one small protocol. Generators let you speak it in a few lines, and
with them you can process a 20 GB file in a few megabytes of memory.

## The iteration protocol

| Concept | Has | Example |
| --- | --- | --- |
| **Iterable** | `__iter__()`, which returns an iterator | `list`, `str`, `dict`, `range`, a file |
| **Iterator** | `__next__()`, which returns the next item or raises `StopIteration`; and `__iter__()` returning itself | what `iter(list)` returns, a file object, a generator |

A `for` loop is exactly this:

```python
iterator = iter(items)
while True:
    try:
        item = next(iterator)
    except StopIteration:
        break
    ...  # the loop body
```

An iterable can be iterated many times (each `iter()` starts over); **an iterator is
exhausted after one pass**:

```python
squares = map(lambda n: n * n, [1, 2, 3])   # map returns an iterator
list(squares)   # [1, 4, 9]
list(squares)   # []  <- already consumed
```

## Generator functions

A function with `yield` in it returns a **generator** when called. Nothing in the body runs
until you ask for the first item; each `yield` hands one value out and **suspends** the
function, keeping its local variables, until the next `next()`:

```python
def countdown(n: int) -> Iterator[int]:
    print("starting")
    while n > 0:
        yield n
        n -= 1
    print("done")

gen = countdown(3)    # prints nothing yet
next(gen)             # prints "starting", returns 3
list(gen)             # [2, 1], then prints "done"
```

A `return` in a generator ends it (the value is carried by `StopIteration.value`, which
`yield from` passes on). Annotate generators as `Iterator[T]` (or `Generator[T, None, R]`
when you use `send` or a return value).

`yield from` delegates to another iterable:

```python
def walk(tree: dict) -> Iterator[str]:
    for name, child in tree.items():
        yield name
        yield from walk(child)
```

**Generator expressions** are the one-line form: `(line.strip() for line in file)`.

## Lazy pipelines

Chain generators and each stage pulls one item at a time from the previous one. Nothing is
stored except the item in flight:

```python
def read(path: Path) -> Iterator[str]:
    with path.open(encoding="utf-8") as file:
        for line in file:                    # a file object yields lines lazily
            yield line.rstrip("\n")

def parse(lines: Iterable[str]) -> Iterator[Event]:
    for line in lines:
        if line and not line.startswith("#"):
            yield Event.from_line(line)

def in_country(events: Iterable[Event], country: str) -> Iterator[Event]:
    return (event for event in events if event.country == country)

totals: Counter[str] = Counter()
for event in in_country(parse(read(Path("events.log"))), "BR"):
    totals[event.day] += event.amount          # memory: one event + one entry per day
```

Compare `list(open(path))`: every line in memory at once. A list of a million short strings
costs well over 50 MB; the generator pipeline costs the same for a thousand lines or a
billion.

Laziness also means **infinite** sequences are fine, as long as the consumer stops:

```python
from itertools import count, islice
evens = (n for n in count() if n % 2 == 0)
list(islice(evens, 5))    # [0, 2, 4, 6, 8]
```

## itertools: the standard toolkit

| Function | Does |
| --- | --- |
| `islice(it, start, stop)` | Slices any iterator: `islice(lines, 10)` is the first ten |
| `chain(a, b)` / `chain.from_iterable(xs)` | Concatenates iterables (since 3.15 also `(*x for x in xs)`) |
| `groupby(it, key)` | Groups **consecutive** items with the same key: sort or group the data first |
| `batched(it, n)` (3.12) | Tuples of up to `n` items, for bulk inserts and API batches |
| `pairwise(it)` | `(a, b), (b, c), ...`: differences between neighbours |
| `accumulate(it)` | Running totals |
| `takewhile` / `dropwhile` | Take or skip while a condition holds |
| `count`, `cycle`, `repeat` | Infinite sources |
| `tee(it, n)` | Several independent iterators over one (it buffers: use with care) |

```python
from itertools import batched, groupby, pairwise

for chunk in batched(rows, 500):
    db.insert_many(chunk)

for day, events in groupby(sorted_events, key=lambda e: e.day):
    print(day, sum(e.amount for e in events))

gaps = [b.time - a.time for a, b in pairwise(events)]
```

## Traps

**Exhausted iterators.** A function that loops twice over its argument breaks when given a
generator. Either accept a `Sequence` (and say so in the type) or materialise once with
`items = list(items)`.

**Deferred errors.** Code in a generator function runs only when items are pulled, so argument
checks run late too:

```python
def chunks(items, size):
    if size < 1:
        raise ValueError("size must be >= 1")   # raised at the first next(), not at the call
    ...
```

To fail at the call, validate in a normal function and return the generator from an inner
one:

```python
def chunks(items: Iterable[T], size: int) -> Iterator[list[T]]:
    if size < 1:
        raise ValueError("size must be >= 1")   # raised now
    return _chunks(items, size)
```

**Resources.** A generator that opens a file in a `with` block keeps it open until it is
exhausted or closed. If a consumer stops early, call `gen.close()` (or wrap the generator in
`contextlib.closing`) so the `with` block exits.

## Generators that receive values

Generators can also receive values with `gen.send(value)` (`yield` becomes an expression),
and be stopped from outside with `gen.throw()` and `gen.close()`. That machinery was the base
of early asyncio coroutines; today `async def` replaces it, and you will rarely write `send`
yourself.

## Summary

- Iterables give iterators; iterators give items until `StopIteration`, and only once.
- `yield` makes a generator: lazy, suspended between items, cheap in memory; `yield from`
  delegates.
- Chain generators into pipelines; consume them with `for`, `sum`, `Counter`, `islice`.
- `itertools` covers slicing, chaining, grouping (consecutive only), batching and pairs.
- Watch for exhausted iterators, errors that surface on the first `next()`, and files kept
  open by unfinished generators.

## Further reading

- Functional Programming HOWTO, "Iterators" and "Generators": <https://docs.python.org/3/howto/functional.html>
- `itertools`, functions creating iterators: <https://docs.python.org/3/library/itertools.html>
- The Language Reference, "Yield expressions": <https://docs.python.org/3/reference/expressions.html#yield-expressions>
- PEP 255, simple generators: <https://peps.python.org/pep-0255/>
- PEP 380, delegating with yield from: <https://peps.python.org/pep-0380/>
- roadmap.sh, Python topic "Iterators": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/iterators@aB1LSQjDEQb_BxueOcnxU.md>
