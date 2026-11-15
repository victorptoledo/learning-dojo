---
from: javascript
status: draft
concept: "Hash tables"
from_way: "Using plain objects as maps, so keys become strings and \"__proto__\" or \"constructor\" collide with built-ins."
native_way: "Map and Set are real hash tables that keep insertion order and accept any key. Object keys are compared by identity (SameValueZero), not by contents: there is no hashCode or equals to override."
sensei_tip: "To key a Map by a composite value, build a string key such as `${userId}|${device}`, or keep nested Maps. JavaScript has no equivalent of a value-based record key yet, so two `{x: 1}` objects are two different keys."
---

| JavaScript | Java | Note |
| --- | --- | --- |
| `new Map()` | `LinkedHashMap` | any key, insertion order |
| `new Set()` | `LinkedHashSet` | any value, insertion order |
| `{}` as a map | `HashMap<String, V>` | string keys only, prototype pitfalls |
| object keys in a Map | `IdentityHashMap` | compared by identity |
| `` `${a}|${b}` `` key | `record Key(a, b)` | composite key |
| `WeakMap` | `WeakHashMap` | keys do not keep objects alive |
