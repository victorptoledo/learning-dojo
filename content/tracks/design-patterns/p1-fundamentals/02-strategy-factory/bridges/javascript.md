---
from: javascript
status: draft
concept: "Strategy and factory"
from_way: "A long `switch (method)` in one function, or a class hierarchy copied from Java."
native_way: "An object literal mapping names to functions is strategy, factory and registry at once; pass functions as parameters for one-off strategies."
sensei_tip: "`const rules = { express, standard }; rules[method]?.(parcel) ?? fail()` is the whole pattern. Freeze the object if other modules must not add to it."
---

| JavaScript | Pattern |
| --- | --- |
| `arr.sort(compareFn)` | strategy |
| `{ express: fn, standard: fn }` | registry |
| `createClient(options)` factory functions | factory |
| `Map` with string keys | registry with any key |
| passport.js strategies | named strategies registered at startup |
