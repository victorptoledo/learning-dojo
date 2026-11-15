# Functions, Arguments, Scopes and Closures

Functions in Python are objects like any other: you can store them, pass them, return them
and build them at run time. Their parameter lists are also unusually expressive. This lesson
covers both, plus the two classic traps every Python developer meets once.

## Defining and calling

```python
def area(width: float, height: float = 1.0) -> float:
    """Return the area of a rectangle."""
    return width * height

area(3, 4)                 # positional
area(width=3, height=4)    # keyword: any order
area(3)                    # height uses its default
```

A function without `return` (or with a bare `return`) returns `None`. The first string in
the body is the **docstring**, shown by `help(area)` and by editors.

## The full parameter list

```python
def report(source, /, title, *rows, sep=", ", **options):
    ...
```

| Part | Meaning |
| --- | --- |
| `source, /` | **Positional-only**: callers cannot write `source=...`, so you may rename it later |
| `title` | Positional or keyword |
| `*rows` | Collects any extra positional arguments into a **tuple** |
| `sep=", "` (after `*rows` or a bare `*`) | **Keyword-only**: must be passed by name |
| `**options` | Collects any extra keyword arguments into a **dict** |

Keyword-only parameters are the best tool for readable calls. Compare `connect(host, 5432,
True, False)` with:

```python
def connect(host: str, port: int = 5432, *, ssl: bool = True, retry: bool = False): ...

connect("db", ssl=False)     # the call says what it does
```

The same stars **unpack** at the call site:

```python
args = ("db", 6432)
options = {"ssl": False}
connect(*args, **options)     # the same as connect("db", 6432, ssl=False)
print(*["a", "b"], sep="-")   # a-b
```

## Trap 1: mutable default arguments

A default value is evaluated **once**, when `def` runs, and stored on the function. A mutable
default is therefore shared by every call:

```python
def add_tag(tag, tags=[]):      # one list, created once
    tags.append(tag)
    return tags

add_tag("a")   # ['a']
add_tag("b")   # ['a', 'b']  <- surprise
```

Use `None` as the default and create the object inside. (ruff reports this as B006, and the
dojo's checks do too.)

```python
def add_tag(tag: str, tags: list[str] | None = None) -> list[str]:
    tags = [] if tags is None else tags
    tags.append(tag)
    return tags
```

When `None` is itself a meaningful value for the caller, use a **sentinel** instead: since
3.15 `MISSING = sentinel("MISSING")` is built in (older code uses `MISSING = object()`), and
the function tests `if value is MISSING:`.

## Scopes: LEGB

A name is looked up in four scopes, in order: **L**ocal (the current function),
**E**nclosing (outer functions), **G**lobal (the module) and **B**uilt-in (`len`, `print`).

Assigning to a name anywhere in a function makes it **local in the whole function**, which
explains this error:

```python
count = 0
def bump():
    count += 1        # UnboundLocalError: count is local here, and not yet assigned
```

`global count` would make it refer to the module variable, and `nonlocal` does the same for
an enclosing function's variable. Both are signs to reconsider the design: return values
instead, or keep state in an object.

## Functions are values

```python
def shout(text: str) -> str:
    return text.upper()

handlers = {"shout": shout, "quiet": str.lower}
handlers["shout"]("hi")              # 'HI'
sorted(names, key=len)               # pass a function
max(products, key=lambda p: p.price) # a lambda is a one-expression function
```

A **lambda** is limited to one expression and has no name in tracebacks; for anything longer,
write a `def`. `functools.partial` fixes some arguments of a function and returns a new
callable:

```python
from functools import partial

parse_hex = partial(int, base=16)
parse_hex("ff")   # 255
```

## Closures

A function defined inside another one can use the outer function's variables, even after the
outer function has returned. Together they form a **closure**:

```python
def make_counter(start: int = 0):
    count = start
    def increment(step: int = 1) -> int:
        nonlocal count          # rebind the enclosing variable
        count += step
        return count
    return increment

next_id = make_counter(100)
next_id(), next_id()   # (101, 102)
```

Closures are how decorators work (next phase), and a light alternative to a class with one
method.

### Trap 2: late binding

A closure captures **variables, not values**. The value is looked up when the function is
called:

```python
handlers = [lambda: i for i in range(3)]
[h() for h in handlers]        # [2, 2, 2]: every lambda sees the final i
```

Bind the current value when the function is created, with a default argument or `partial`:

```python
handlers = [lambda i=i: i for i in range(3)]          # [0, 1, 2]
handlers = [partial(lambda i: i, i) for i in range(3)]
```

## Recursion

Python supports recursion but has **no tail-call optimisation**, and the default recursion
limit is about 1000 frames (`sys.getrecursionlimit()`). Walking a deep structure is usually
written with an explicit stack (a list) instead.

## Summary

- Parameters: positional-only before `/`, `*args`, keyword-only after `*`, `**kwargs`.
- Defaults are evaluated once: never use a mutable default, use `None`.
- Names resolve Local, Enclosing, Global, Built-in; assignment makes a name local.
- Functions are values: pass them as keys and callbacks, fix arguments with `partial`.
- Closures capture variables, not values: bind loop variables explicitly.

## Further reading

- The Python Tutorial, "Defining Functions" and "More on Defining Functions": <https://docs.python.org/3/tutorial/controlflow.html#defining-functions>
- Programming FAQ, "Why are default values shared between objects?": <https://docs.python.org/3/faq/programming.html#why-are-default-values-shared-between-objects>
- PEP 3102, keyword-only arguments: <https://peps.python.org/pep-3102/>
- PEP 570, positional-only parameters: <https://peps.python.org/pep-0570/>
- `functools`: <https://docs.python.org/3/library/functools.html>
- roadmap.sh, Python topic "Variable Scope": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/variable-scope@3RNy7Sp28d-NMx0Yh4bdx.md>
