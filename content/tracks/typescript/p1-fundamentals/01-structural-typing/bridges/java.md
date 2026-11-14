---
from: java
status: draft
concept: "Type system"
from_way: "Nominal typing: two classes with identical fields but different names are not compatible."
native_way: "Structural typing: if the shape matches, the types are interchangeable, with no explicit inheritance."
sensei_tip: "Avoid type assertions (`as Type`). Use narrowing (`if ('prop' in obj)`) or type guards for real runtime safety."
---
