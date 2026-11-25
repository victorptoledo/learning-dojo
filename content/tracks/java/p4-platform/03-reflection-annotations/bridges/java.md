---
from: java
status: draft
concept: "Annotations and reflection"
from_way: "Annotations as framework magic, and reflection through getDeclaredFields with setAccessible(true)."
native_way: "Your own RUNTIME annotations, record components, canonical constructors and lookups cached once."
sensei_tip: "With records you rarely need setAccessible: components, accessors and the canonical constructor are public and ordered."
---

| Habit | Java 25 |
| --- | --- |
| `getDeclaredFields() (order unspecified)` | `getRecordComponents() (declaration order)` |
| `field.setAccessible(true); field.get(o)` | `component.getAccessor().invoke(o)` |
| `catch (Exception e) { throw e; }` | `unwrap InvocationTargetException.getCause()` |
| `lookups on every call` | `a mapper built once per class` |
