---
from: javascript
status: draft
concept: "Creational patterns"
from_way: "Constructors with long positional parameter lists, or builder classes copied from Java."
native_way: "An options object with defaults (`createClient({ url, timeout = 10 })`) replaces builders, factory functions replace constructors, and spreading (`{ ...base, retries: 5 }`) replaces clone."
sensei_tip: "`structuredClone(obj)` makes a deep copy of plain data; for immutable updates, the spread syntax is the idiomatic prototype."
---

| JavaScript | Pattern |
| --- | --- |
| `fn({ url, method = "GET" })` | builder (options object) |
| factory functions | factory |
| an exported instance from an ES module | singleton |
| `{ ...base, retries: 5 }`, `structuredClone` | prototype |
| `Object.freeze` | immutable value |
