---
from: java
status: draft
concept: "References, value types and control flow"
from_way: "Primitives (int, double) copied by value, objects reached through references, == for identity and equals() for value, a switch on constants."
native_way: "Everything is an object reached through a name; == calls __eq__ (value) and is compares identity; match dispatches on the shape of data."
sensei_tip: "Read Python's == as Java's equals() and Python's is as Java's ==. And int never overflows: there is no long, BigInteger or Integer cache to think about."
---

| Java | Python |
| --- | --- |
| `a.equals(b)` / `a == b` | `a == b` / `a is b` |
| `int`, `long`, `BigInteger` | `int` (unbounded) |
| `BigDecimal` | `decimal.Decimal("19.99")` |
| `String.format("%.2f", x)` | `f"{x:.2f}"` |
| `for (int i = 0; i < xs.size(); i++)` | `for i, x in enumerate(xs):` |
| `switch` with patterns | `match` / `case` |
| `null` | `None` |
| `{ ... }` blocks | indentation and `:` |
