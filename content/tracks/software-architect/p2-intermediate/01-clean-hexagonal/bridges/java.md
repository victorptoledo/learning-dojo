---
from: java
status: draft
concept: "Separation of concerns and ports"
from_way: "Annotating domain entities with ORM annotations, tying business rules to the persistence library."
native_way: "Pure domain entities with no external dependency; the database is just a secondary adapter that implements a port."
sensei_tip: "Golden rule: swapping the web framework or the database must not change a single line in the domain folder."
---
