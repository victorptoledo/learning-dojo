---
from: javascript
status: draft
concept: "Closed sets of alternatives"
from_way: "TypeScript discriminated unions with a kind field and switch."
native_way: "Sealed hierarchies of records with pattern matching."
sensei_tip: "A TypeScript union with kind: 'circle' | 'square' is a sealed interface; the switch on kind is a switch with record patterns, and both check exhaustiveness."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| type Shape = Circle \| Square | sealed interface Shape permits Circle, Square |
| switch (s.kind) | switch (s) |
| case 'circle': s.radius | case Circle(var radius) -> |
| const _: never = s | exhaustive switch |
