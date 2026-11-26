---
from: python
status: draft
concept: "Modern text handling"
from_way: "lower() for comparisons, re.match to validate, string concatenation into patterns, and Python 2 habits of mixing bytes and text."
native_way: "casefold() and NFC/NFKC keys, re.fullmatch, named groups, re.escape, removeprefix and removesuffix, and atomic groups or possessive quantifiers (3.11+)."
sensei_tip: "If two strings look the same and compare different, print [hex(ord(c)) for c in s] for both; the answer is almost always normalisation."
---

| Older habit | Python 3.15 |
| --- | --- |
| `a.lower() == b.lower()` | `key(a) == key(b)` with NFKC + `casefold()` |
| `re.match(p, s)` to validate | `re.fullmatch(p, s)` |
| `m.group(1)` | named groups: `m["level"]` |
| `re.compile(user_text)` | `re.compile(re.escape(user_text))` |
| `s.strip("prefix")` | `s.removeprefix("prefix")` |
| hoping a pattern is fast | `(?>...)`, `a++`, or a pattern with one way to match |
