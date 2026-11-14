---
from: java
status: draft
concept: "Error handling"
from_way: "Exception hierarchies with try/catch/finally blocks that hide error paths."
native_way: "Functions return `(value, error)` and callers check explicitly: `if err != nil { return nil, err }`."
sensei_tip: "It feels repetitive at first, but it makes any codebase predictable: no surprise exception takes the program down."
---
