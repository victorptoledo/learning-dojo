---
from: java
status: draft
concept: "Composition, coupling and cohesion"
from_way: "Deep hierarchies (`AbstractBaseService` → `AbstractCrudService` → `UserService`), protected fields shared down the tree, static utility classes everywhere."
native_way: "Final classes that hold their collaborators in fields, records for data, small interfaces for roles, and inheritance only for sealed sets of variants."
sensei_tip: "Effective Java's 'favor composition over inheritance' item uses exactly the HashSet.addAll trap. When in doubt, make the class final; you can always open it later, never close it."
---

| Java | Idea |
| --- | --- |
| `final class` by default, records, enums | composition over inheritance |
| a wrapper holding a `Set<E>` | forwarding instead of subclassing |
| constructor-injected interface fields | message coupling |
| `public static` mutable state | common coupling (avoid) |
| a method that takes a boolean flag | control coupling (avoid) |
