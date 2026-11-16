---
from: python
status: draft
concept: "Regular expressions in Python"
from_way: "Assuming re is always fast."
native_way: "re is a backtracking engine; Python 3.11 added atomic groups and possessive quantifiers; the third-party regex module adds timeouts; google-re2 offers linear-time matching."
sensei_tip: "Use `re.fullmatch` for validation, avoid nested quantifiers, and for user-supplied patterns or input consider the `google-re2` package."
---

| In Python | Underneath |
| --- | --- |
| `re.compile(p)` | parsed and cached |
| `re.fullmatch` | whole-string match |
| `(?>...)`, `a*+` (3.11+) | atomic group, possessive |
| `regex.match(p, s, timeout=0.1)` | a time limit (third-party regex) |
| `re2.match` | linear time |
