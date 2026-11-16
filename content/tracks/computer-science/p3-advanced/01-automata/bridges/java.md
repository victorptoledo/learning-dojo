---
from: java
status: draft
concept: "Regular expressions in Java"
from_way: "Validating untrusted input with any java.util.regex pattern."
native_way: "java.util.regex is a backtracking engine: it supports backreferences and lookaround, but nested quantifiers can take exponential time; possessive quantifiers and atomic groups cut backtracking."
sensei_tip: "For untrusted input, prefer patterns without nested quantifiers, use possessive forms (`\\w++`), limit input length, or use a linear-time engine such as RE2/J."
---

| In Java | Underneath |
| --- | --- |
| `Pattern.compile(p)` | parsed once, reuse it |
| `(a+)+$` | catastrophic backtracking |
| `a++`, `(?>a+)` | possessive and atomic: no backtracking into them |
| `matcher.matches()` | whole-string match |
| RE2/J | automaton-based, linear time |
