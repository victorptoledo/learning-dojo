---
from: go
status: draft
concept: "Annotations and reflection"
from_way: "Struct tags such as csv:\"hours\" read with the reflect package."
native_way: "Annotations with typed elements read with reflection."
sensei_tip: "A struct tag is a string you parse; an annotation is typed, checked by the compiler and can have defaults."
---

| Go | Java 25 |
| --- | --- |
| `csv:"hours" struct tag` | `@Column("hours")` |
| `Field(i).Tag.Get("csv")` | `component.getAnnotation(Column.class).value()` |
| `reflect.ValueOf(v).Field(i)` | `component.getAccessor().invoke(v)` |
| `reflect.New(t)` | `constructor.newInstance(args)` |
