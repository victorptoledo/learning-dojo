---
from: go
status: draft
concept: "Choosing patterns"
from_way: "Porting Java patterns literally: factories, getters and interfaces declared next to their only implementation."
native_way: "Choosing from the forces with Go's tools: small consumer interfaces, functions as values, struct composition, middleware and explicit wiring in main; very few named patterns."
sensei_tip: "'Clear is better than clever' is a Go proverb and a pattern filter: if a pattern makes the call site harder to read, Go code usually goes without it."
---

| Problem | Go choice |
| --- | --- |
| varying algorithm | function types, small interfaces |
| cross-cutting behaviour | wrapper types, middleware |
| closed variants | interface + type switch (with a linter) |
| reacting to facts | channels, callbacks |
| foreign model | adapter package behind a consumer interface |
| combinable rules | predicate funcs, query builders |
