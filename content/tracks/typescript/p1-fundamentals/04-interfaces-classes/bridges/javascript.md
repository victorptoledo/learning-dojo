---
from: javascript
status: draft
concept: "Classes and functions with types"
from_way: "Classes with #private fields, getters and extends; functions with default and rest parameters; no way to state a callback's shape."
native_way: "The same classes and functions, plus parameter and return types, interfaces to describe shapes, implements, override and overload signatures."
sensei_tip: "Everything you know about JavaScript classes still applies, #private fields included. TypeScript adds checks on top; avoid its extras that generate code (parameter properties, enums)."
---

| JavaScript | TypeScript |
| --- | --- |
| `class A { #x = 1 }` | The same, with `#x: number = 1` if you like |
| `function f(a, b = 1, ...rest)` | `function f(a: string, b = 1, ...rest: number[])` |
| JSDoc `@callback` | `type Handler = (event: Event) => void` |
| `if (Array.isArray(x)) ...` to accept two forms | Overload signatures or a union parameter |
