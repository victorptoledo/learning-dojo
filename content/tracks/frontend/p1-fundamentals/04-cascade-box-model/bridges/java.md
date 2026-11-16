---
from: java
status: draft
concept: "Resolution rules like method overload resolution"
from_way: "The compiler picks the most specific overload, and a subclass override beats the parent."
native_way: "The cascade picks the winning declaration by origin, layer, specificity and finally source order."
sensei_tip: "Like overloads, specificity is predictable once you can compute it; keep selectors as flat as you keep class hierarchies."
---

| Java | In the browser |
| --- | --- |
| Most specific overload wins | Highest specificity wins |
| Override in a subclass | A later rule of equal specificity |
| Module layers in an architecture | `@layer` order |
| Fields inherited from a superclass | Inherited properties like color and font |
