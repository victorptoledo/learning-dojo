---
from: javascript
status: draft
concept: "A bounded state for a long-lived app"
from_way: "Redux stores with normalised state, pagination instead of loading everything, and lazy-loaded modules."
native_way: "Keep the context like a lean store: references (paths, ids) instead of whole documents, results loaded just in time, old payloads replaced by placeholders and old history folded into a summary."
sensei_tip: "Write compaction as a pure function from (system, turns, budget) to a new context, like a reducer, and test it with fixed inputs; a fake summariser makes the tests deterministic."
---

| JavaScript | Context-engineering concept |
| --- | --- |
| a reducer returning new state | compaction as a pure function |
| normalised ids instead of nested objects | lightweight references instead of whole documents |
| lazy `import()` | just-in-time retrieval |
| `localStorage` between sessions | notes that survive a new session |
| a Web Worker with its own memory | a sub-agent with a fresh context |
