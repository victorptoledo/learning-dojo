---
from: go
status: draft
concept: "Regular expressions in Go"
from_way: "Missing backreferences and lookaround in Go's regexp."
native_way: "Go's regexp implements RE2: it builds NFAs and DFAs and guarantees linear time, which is why it rejects backreferences and lookaround."
sensei_tip: "Go's regexp is safe on untrusted input by design; when you need features it lacks, write a small parser instead of a cleverer pattern."
---

| In Go | Underneath |
| --- | --- |
| `regexp.MustCompile(p)` | compile once at package level |
| `(a*)*b` | still linear |
| `\1` | a compile error: not regular |
| `re.FindStringIndex` | leftmost-first match positions |
| `regexp/syntax` | the parser and the compiled program |
