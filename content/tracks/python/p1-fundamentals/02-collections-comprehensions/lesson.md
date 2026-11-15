# Lists, Tuples, Dicts, Sets and Comprehensions

Most Python programs are built from four collections: **list**, **tuple**, **dict** and
**set**. Knowing what each operation costs, and the handful of helpers in `collections`, is
the difference between a script that takes a second and one that takes an hour.

## The four built-ins

| Type | Literal | Ordered | Mutable | Typical use |
| --- | --- | --- | --- | --- |
| `list` | `[1, 2, 3]` | yes | yes | A sequence you append to and iterate |
| `tuple` | `(1, 2)` or `1, 2` | yes | no | A fixed record: a point, a row, a return of several values |
| `dict` | `{"a": 1}` | insertion order | yes | Lookup by key |
| `set` | `{1, 2}` (`set()` for empty) | no | yes | Membership and de-duplication |

What they cost (average case):

| Operation | list | dict / set |
| --- | --- | --- |
| `x[i]` / `d[key]` | O(1) | O(1) |
| `x in collection` | **O(n)**: scans | **O(1)**: hashes |
| append / add | O(1) amortised | O(1) |
| insert or delete at the front | O(n) | n/a |
| `sort()` | O(n log n), stable | n/a |

The rule that follows: **if you test membership in a loop, build a set first.**

```python
banned = set(load_banned_ids())          # once: O(n)
clean = [u for u in users if u.id not in banned]   # then O(1) per check
```

### Hashable keys

Dict keys and set items must be **hashable**: their hash never changes and equal objects
have equal hashes. Immutable built-ins are hashable (`str`, `int`, `tuple` of hashables,
`frozenset`); `list`, `dict` and `set` are not. That is why you use a tuple as a compound key:

```python
grid: dict[tuple[int, int], str] = {(0, 0): "X"}
```

## Lists, slicing and unpacking

```python
xs = [10, 20, 30, 40, 50]
xs[-1], xs[1:3], xs[::-1]     # 50, [20, 30], [50, 40, 30, 20, 10]
xs[1:3] = [21, 31]            # slice assignment
first, *middle, last = xs     # star unpacking: middle == [21, 31, 40]
a, b = b, a                   # swap with a tuple
```

A slice is a **shallow copy**. To remove items while iterating, build a new list instead of
deleting from the one you loop over.

## Sorting with keys

`sorted(iterable)` returns a new list; `xs.sort()` sorts in place and returns `None`. Both
take a **key** function and are **stable**: equal keys keep their order.

```python
people.sort(key=lambda p: p.age)
sorted(words, key=str.casefold)
# most frequent first, ties alphabetically: sort by a tuple
sorted(counts.items(), key=lambda item: (-item[1], item[0]))
```

`operator.itemgetter(1)` and `operator.attrgetter("age")` are faster, readable alternatives to
small lambdas.

## Dicts

```python
stock = {"apple": 3, "pear": 0}
stock.get("kiwi", 0)                 # 0: no KeyError
stock.setdefault("kiwi", 5)          # inserts and returns 5 only if missing
for name, qty in stock.items(): ...  # never `for k in d: d[k]`
merged = defaults | overrides        # 3.9+: a new dict, right side wins
stock |= {"plum": 2}                 # update in place
```

Dicts keep **insertion order** (guaranteed since 3.7), so `dict.fromkeys(items)` is the
idiomatic way to de-duplicate while keeping the first occurrence.

Since 3.15, **`frozendict`** is a built-in immutable dict: it keeps insertion order, refuses
changes, and is hashable when its keys and values are, so it can be a dict key, a set item or
a safely shared default.

```python
limits = frozendict(cpu=2, memory="1g")
limits["cpu"] = 4                     # TypeError: no item assignment
cache = {limits: "small"}             # hashable, so it works as a key
```

## Sets

```python
a, b = {"py", "rs", "go"}, {"go", "java"}
a | b, a & b, a - b, a ^ b     # union, intersection, difference, symmetric difference
a <= b                         # subset
frozenset(a)                   # immutable and hashable: can be a dict key
```

## The collections module

| Tool | Use it for |
| --- | --- |
| `Counter` | Counting: `Counter(words).most_common(3)`, arithmetic between counts |
| `defaultdict(list)` | Grouping: `groups[key].append(item)` without checking if the key exists |
| `deque(maxlen=n)` | A queue with O(1) at both ends, and a bounded "last n items" buffer |
| `namedtuple` | A lightweight immutable record (a dataclass is usually nicer, see later) |
| `ChainMap` | Layered lookups: command line over environment over defaults |

`heapq` (a priority queue on a list) and `bisect` (binary search in a sorted list) complete
the toolbox.

```python
from collections import Counter, defaultdict

by_country = defaultdict(list)
for order in orders:
    by_country[order.country].append(order)

Counter("mississippi").most_common(2)   # [('i', 4), ('s', 4)]
```

> [!WARNING]
> `most_common` breaks ties by first occurrence, not alphabetically. When ties must be
> ordered, sort yourself with a tuple key.

## Comprehensions

A comprehension builds a collection from an iterable, with an optional filter:

```python
squares = [n * n for n in range(10)]
evens = [n for n in numbers if n % 2 == 0]
lengths = {word: len(word) for word in words}        # dict comprehension
initials = {name[0] for name in names}               # set comprehension
pairs = [(x, y) for x in xs for y in ys if x != y]   # nested: same order as the loops
labels = ["even" if n % 2 == 0 else "odd" for n in numbers]   # expression, not filter
```

Since 3.15, `*` and `**` can unpack inside a comprehension (PEP 798), which flattens and merges
without a nested loop:

```python
[*row for row in matrix]          # flatten: same as [x for row in matrix for x in row]
{*tags for tags in posts}         # union of sets
{**layer for layer in configs}    # merge dicts, later layers win
```

A **generator expression** (parentheses instead of brackets) produces items lazily, one at a
time; pass it straight to a function that consumes an iterable:

```python
total = sum(order.amount for order in orders)   # no list is built
any(user.is_admin for user in users)            # stops at the first True
```

Comprehensions are for **building** collections. When the body has side effects, several
statements or more than two `for` and `if` clauses, a plain loop is clearer.

## Summary

- list for sequences, tuple for fixed records, dict for lookup by key, set for membership.
- `in` is O(n) on a list and O(1) on a set or dict: build a set for repeated checks.
- Sort with `key=` and tuple keys for tie rules; sorting is stable.
- `Counter`, `defaultdict` and `deque` remove most hand-written bookkeeping.
- Comprehensions build collections; generator expressions feed consumers without a list.

## Further reading

- The Python Tutorial, "Data Structures": <https://docs.python.org/3/tutorial/datastructures.html>
- `collections`, container datatypes: <https://docs.python.org/3/library/collections.html>
- Sorting Techniques (HOWTO): <https://docs.python.org/3/howto/sorting.html>
- Time complexity of CPython operations: <https://wiki.python.org/moin/TimeComplexity>
- roadmap.sh, Python topic "List Comprehensions": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/list-comprehensions@4gtmtYWYRWqwLdZRL0XMg.md>
