---
from: python
status: draft
concept: "String matching"
from_way: "`text.find(p, start)` in a loop, or `p in text`."
native_way: "CPython's `str.find` and `in` use a mix of Boyer-Moore-Horspool and, since 3.10, the Crochemore-Perrin two-way algorithm for long patterns, so a single search is linear. Overlapping counts still need your own loop, and `str.count` counts non-overlapping matches only."
sensei_tip: "`'aaaa'.count('aa')` is 2, not 3. For overlapping matches, write KMP or use `re.finditer('(?=aa)', text)`, whose lookahead consumes nothing."
---

| Python | Java |
| --- | --- |
| `text.find(p, start)` | `text.indexOf(p, start)` |
| `text.count(p)` (non-overlapping) | a loop resuming after the match |
| `re.finditer('(?=' + re.escape(p) + ')', t)` | KMP for overlapping matches |
| ints never overflow | `long` arithmetic modulo M |
| `hash(s)` (randomised per process) | `String.hashCode()` (fixed, easy to collide) |
