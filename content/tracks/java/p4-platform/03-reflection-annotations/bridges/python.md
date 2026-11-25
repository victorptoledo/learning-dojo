---
from: python
status: draft
concept: "Annotations and reflection"
from_way: "Decorators, dataclasses.fields() and getattr."
native_way: "Annotations as passive data read with reflection, and record components."
sensei_tip: "A Python decorator runs code; a Java annotation is only data until a tool reads it with RUNTIME retention."
---

| Python | Java 25 |
| --- | --- |
| `@dataclass fields()` | `Record.class.getRecordComponents()` |
| `field.metadata["column"]` | `component.getAnnotation(Column.class).value()` |
| `getattr(obj, name)` | `component.getAccessor().invoke(obj)` |
| `cls(**kwargs)` | `canonical constructor.newInstance(args)` |
