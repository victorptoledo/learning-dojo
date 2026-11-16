---
from: javascript
status: draft
concept: "Regular expressions in JavaScript"
from_way: "Running user-provided input through a complex RegExp on the server's event loop."
native_way: "V8's Irregexp is a backtracking engine compiled to machine code; a catastrophic pattern blocks the single event-loop thread; V8 has an experimental linear-time engine (the `l` flag behind a flag)."
sensei_tip: "Treat ReDoS as a denial of service on Node: lint patterns (eslint-plugin-regexp, safe-regex), bound input lengths, or use the re2 npm package."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `/^(a+)+$/.test(s)` | exponential on near-matches |
| `str.match(re)` | leftmost, backtracking |
| `/u` flag | matches by code point |
| `/y` flag | sticky: anchors at lastIndex, handy in lexers |
| `re2` package | linear time |
