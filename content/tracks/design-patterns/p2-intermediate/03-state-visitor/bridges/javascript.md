---
from: javascript
status: draft
concept: "State, visitor, composite and template method"
from_way: "String states compared in if chains, and visitor objects with one method per node type."
native_way: "Tagged objects (`{ kind: 'circle', r }`) with a `switch (x.kind)`, state machine libraries such as XState, and recursive plain objects for composites."
sensei_tip: "With TypeScript, a discriminated union plus a `never` check in the default branch gives you the same exhaustiveness check Java's sealed types give."
---

| JavaScript | Pattern |
| --- | --- |
| `{ kind: "paid", total }` objects | state |
| `switch (node.kind)` | visitor |
| XState machines | state |
| trees of plain objects | composite |
| `[Symbol.iterator]`, generators | iterator |
