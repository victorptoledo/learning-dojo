# Syntax, Built-in Types and the Object Model

Python reads almost like pseudo-code, but under the friendly syntax there is one precise idea
that explains most surprises: **everything is an object, and variables are names that point
at objects.** This lesson builds that model, then covers the built-in types and control flow
you will use in every program.

## Names, objects, identity

Assignment never copies. It **binds a name to an object**:

```python
a = [1, 2, 3]
b = a            # a second name for the same list
b.append(4)
a                # [1, 2, 3, 4]
a is b           # True: same object (same id())
a == [1, 2, 3, 4]   # True: equal value
```

Every object has an **identity** (`id(x)`, never changes), a **type** (`type(x)`) and a
**value**. `==` compares values; `is` compares identity. Use `is` only for singletons:
`x is None`, `x is True` in tests, and sentinel objects you create yourself
(since 3.15 there is a built-in for them: `MISSING = sentinel("MISSING")`).

Python is **dynamically** typed (a name can point at any type over time) but **strongly**
typed (`"1" + 1` raises `TypeError`; nothing converts silently).

### Mutable and immutable

| Immutable (cannot change after creation) | Mutable |
| --- | --- |
| `int`, `float`, `bool`, `str`, `bytes`, `tuple`, `frozenset`, `None` | `list`, `dict`, `set`, `bytearray`, most of your own classes |

"Changing" an immutable value creates a new object and rebinds the name:

```python
s = "dojo"
t = s
s += "!"      # a new string; t still points at "dojo"
```

With mutable objects, every name sees every change. Copy explicitly when you need
independence: `list(xs)`, `xs.copy()`, `dict(d)` and `copy.copy(x)` are **shallow** (the
container is new, the items are shared); `copy.deepcopy(x)` copies all the way down.

## Numbers

```python
2 ** 100          # 1267650600228229401496703205376: int has no size limit
7 // 2, 7 % 2     # (3, 1): floor division and remainder
-7 // 2           # -4: floor, not truncation
0.1 + 0.2         # 0.30000000000000004: float is IEEE 754 binary
round(2.675, 2)   # 2.67, for the same reason
```

For money, use `decimal.Decimal("19.99")` (built from a **string**, not a float) or integer
cents. `fractions.Fraction(1, 3)` is exact rational arithmetic. `bool` is a subclass of
`int` (`True + True == 2`), which is occasionally useful and often a trap.

## Strings and bytes

`str` is an immutable sequence of Unicode code points; `bytes` is raw data. Encode and decode
at the edges of your program, and always name the encoding:

```python
text = "café"
data = text.encode("utf-8")     # b'caf\xc3\xa9'
data.decode("utf-8")            # 'café'
len(text), len(data)            # (4, 5)
```

Since 3.15, `open()` without an `encoding` argument reads and writes **UTF-8** on every
platform (PEP 686); before, Windows used the locale's code page. Keep writing
`encoding="utf-8"` in code that must also run on older versions.

**f-strings** are the way to format text. The part after `:` is a format specification, and
`=` prints the expression too, which is perfect for debugging:

```python
price, qty, name = 1234.5, 3, "ada"
f"{name.title()} owes {price * qty:,.2f} EUR"   # 'Ada owes 3,703.50 EUR'
f"{qty:>5}|{qty:<5}|{qty:05d}"                  # '    3|3    |00003'
f"{price=}"                                     # 'price=1234.5'
```

Useful methods: `split`, `join`, `strip`, `startswith`, `replace`, `casefold` (for
case-insensitive comparison), `partition`, and `in` for substrings. Strings are immutable,
so building a long one with `+=` in a loop is slow; collect the parts in a list and
`"".join(parts)`.

## Truthiness and boolean operators

In a condition, these are **falsy**: `None`, `False`, `0`, `0.0`, `""`, and empty
containers (`[]`, `{}`, `set()`, `()`). Everything else is truthy, unless a class says
otherwise with `__bool__` or `__len__`.

```python
if not items:            # idiomatic; not "if len(items) == 0"
    ...
name = given or "stranger"   # or returns the first truthy operand, not True/False
0 < x <= 10              # chained comparison: 0 < x and x <= 10
```

`and` and `or` return one of their operands, and stop as soon as the result is known.

## Control flow

Blocks are defined by **indentation** (four spaces), and a colon opens every block.

```python
for index, name in enumerate(names, start=1):   # never range(len(names))
    print(index, name)

for name, score in zip(names, scores, strict=True):   # strict: lengths must match
    ...

while (line := reader.readline()):   # the walrus := assigns inside an expression
    handle(line)

for user in users:
    if user.is_admin:
        break
else:            # runs only when the loop did not break
    raise LookupError("no admin")
```

`range(start, stop, step)` is lazy: `range(10**12)` costs nothing until you iterate.

### match: structural pattern matching

`match` (3.10+) compares a value against **patterns** that describe its shape, and binds
names while it matches:

```python
def describe(event: dict) -> str:
    match event:
        case {"type": "click", "x": int(x), "y": int(y)}:
            return f"click at {x},{y}"
        case {"type": "key", "key": "q" | "Q"}:
            return "quit"
        case {"type": "key", "key": str(key)} if len(key) == 1:
            return f"key {key}"
        case [first, *rest]:
            return f"a batch of {1 + len(rest)} events"
        case _:
            return "unknown"
```

Patterns can be literals, alternatives with `|`, sequences, mappings, class patterns such as
`Point(x=0)`, captures and guards with `if`. A bare name captures; to compare with a
constant, use a dotted name such as `Color.RED`. `match` is not a `switch` on values: reach
for it when the **structure** of the data decides what to do.

## The data model, a first look

Built-in functions and operators call **special methods** ("dunder" methods) on the object:

| You write | Python calls |
| --- | --- |
| `len(x)` | `x.__len__()` |
| `a + b` | `a.__add__(b)` (then `b.__radd__(a)`) |
| `a == b` | `a.__eq__(b)` |
| `x[i]` | `x.__getitem__(i)` |
| `for v in x` | `iter(x)`, which calls `x.__iter__()` |
| `str(x)`, `repr(x)` | `x.__str__()`, `x.__repr__()` |

That is how your own classes become as natural as the built-ins; the protocols lesson goes
deep into it. For now, remember the habit it creates: you call `len(x)`, never `x.__len__()`.

## Style

PEP 8 is the shared style: `snake_case` for functions and variables, `PascalCase` for
classes, `UPPER_CASE` for constants, four spaces, and a docstring as the first statement of a
module, class or function. `ruff format` and `ruff check` apply it for you.

## Summary

- Variables are names bound to objects; assignment never copies. `==` is value, `is` is
  identity.
- Know which types are mutable; copy explicitly (shallow or deep) when you need independence.
- `int` is unbounded, `float` is binary (use `Decimal` for money), `str` is Unicode and
  `bytes` is data.
- f-strings with format specs; truthiness for emptiness checks; `enumerate`, `zip`, `:=`.
- `match` dispatches on the shape of data; operators and built-ins call special methods.

## Further reading

- The Python Tutorial, "More Control Flow Tools": <https://docs.python.org/3/tutorial/controlflow.html>
- The Python Language Reference, "Data model": <https://docs.python.org/3/reference/datamodel.html>
- PEP 636, the pattern matching tutorial: <https://peps.python.org/pep-0636/>
- "Floating-Point Arithmetic: Issues and Limitations": <https://docs.python.org/3/tutorial/floatingpoint.html>
- PEP 8, the style guide: <https://peps.python.org/pep-0008/>
- roadmap.sh, Python topic "Basic Syntax": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/python/content/basic-syntax@6xRncUs3_vxVbDur567QA.md>
