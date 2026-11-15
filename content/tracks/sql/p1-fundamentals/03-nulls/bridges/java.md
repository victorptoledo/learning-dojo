---
from: java
status: draft
concept: "A missing value that is not a value"
from_way: "null references that throw NullPointerException, Optional<T>, and Objects.equals for null-safe comparison."
native_way: "NULL never throws: comparisons with it become UNKNOWN, and WHERE silently drops those rows."
sensei_tip: "Where Java fails loudly, SQL fails quietly. Every time a column can be NULL, ask what each condition and aggregate does with it."
---

| Java | SQL |
| --- | --- |
| x == null | x IS NULL |
| Objects.equals(a, b) | a IS NOT DISTINCT FROM b |
| Optional.ofNullable(nick).orElse(name) | coalesce(nick, name) |
| a != null && a.equals(b) | a = b (UNKNOWN when either is NULL) |
| Integer count = null; count + 1 throws | NULL + 1 is NULL |
