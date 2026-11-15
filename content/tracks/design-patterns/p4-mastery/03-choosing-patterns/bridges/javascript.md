---
from: javascript
status: draft
concept: "Choosing patterns"
from_way: "Class hierarchies and GoF diagrams translated from Java into JavaScript classes."
native_way: "Choosing from the forces with functions, closures, object literals and modules first; classes, events and middleware where the runtime and frameworks already use them."
sensei_tip: "Most JavaScript patterns are a function that returns a function (decorators, middleware, strategies, factories). Reach for a class when you need identity and long-lived state."
---

| Problem | JavaScript choice |
| --- | --- |
| varying algorithm | functions in an object literal |
| cross-cutting behaviour | higher-order functions, middleware |
| closed variants | tagged objects + switch (TypeScript unions) |
| reacting to facts | events, `EventTarget` |
| foreign model | adapter module |
| combinable rules | predicate functions, query-builder objects |
