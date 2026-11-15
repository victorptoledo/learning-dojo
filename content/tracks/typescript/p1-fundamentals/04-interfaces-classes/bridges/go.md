---
from: go
status: draft
concept: "Methods, interfaces and constructors"
from_way: "Structs with methods, implicit interfaces, unexported (lowercase) fields for privacy, constructor functions like NewCache, no overloading."
native_way: "Classes with methods or plain objects plus functions, interfaces also implicit, #private fields, overload signatures when needed."
sensei_tip: "Go's style of plain data plus functions works very well in TypeScript too: types for data, functions for behaviour, and a class only when you need private state."
---

| Go | TypeScript |
| --- | --- |
| `type Cache struct { items map[string]any }` | `class Cache { #items = new Map<string, unknown>() }` |
| `func NewCache(ttl time.Duration) *Cache` | `constructor(ttl: number)` or a `createCache()` factory |
| lowercase field (package-private) | `#field` (class-private) |
| Implicit interface | Structural interface |
| No overloading | Overload signatures |
