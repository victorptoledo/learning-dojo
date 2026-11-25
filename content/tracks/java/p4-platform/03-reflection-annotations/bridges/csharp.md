---
from: csharp
status: draft
concept: "Annotations and reflection"
from_way: "Attributes, typeof(T).GetProperties() and Activator.CreateInstance."
native_way: "Annotations, getRecordComponents() and the canonical constructor."
sensei_tip: "Attributes and annotations are the same idea; Java needs @Retention(RUNTIME), C# keeps attributes by default."
---

| C# | Java 25 |
| --- | --- |
| `[Column("hours")]` | `@Column("hours")` |
| `prop.GetCustomAttribute<Column>()` | `component.getAnnotation(Column.class)` |
| `prop.GetValue(obj)` | `component.getAccessor().invoke(obj)` |
| `TargetInvocationException.InnerException` | `InvocationTargetException.getCause()` |
