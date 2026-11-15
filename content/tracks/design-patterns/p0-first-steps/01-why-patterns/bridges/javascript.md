---
from: javascript
status: draft
concept: "Design patterns"
from_way: "Class hierarchies copied from Java, or the 2010 module pattern built from closures."
native_way: "Functions, closures, object literals and ES modules cover most creational and behavioural patterns; events and promises cover observer."
sensei_tip: "EventEmitter and addEventListener are the observer pattern, Array.prototype.sort(compareFn) is strategy, and an ES module that exports one object is a singleton."
---

| JavaScript | Pattern |
| --- | --- |
| `arr.sort((a, b) => a - b)` | strategy |
| `emitter.on("event", fn)` | observer |
| an ES module exporting one instance | singleton |
| `new Proxy(target, handler)` | proxy |
| factory functions returning objects | factory |
| middleware chains (Express, Koa) | chain of responsibility |
