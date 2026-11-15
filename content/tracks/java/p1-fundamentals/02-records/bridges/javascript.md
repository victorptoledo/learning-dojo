---
from: javascript
status: draft
concept: "Value classes without boilerplate"
from_way: "Plain objects or classes, Object.freeze, and TypeScript readonly properties."
native_way: "Records: immutable, typed and compared by value."
sensei_tip: "Two JavaScript objects with the same fields are never ===; two records with the same values are equals(). That makes them safe keys in a HashMap."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| { x: 1, y: 2 } | new Point(1, 2) |
| readonly fields in TypeScript | record components |
| structural equality by hand | generated equals |
| { ...p, x: 3 } | new Point(3, p.y()) |
