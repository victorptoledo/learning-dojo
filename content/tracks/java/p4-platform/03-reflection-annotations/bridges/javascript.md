---
from: javascript
status: draft
concept: "Annotations and reflection"
from_way: "Decorators (TypeScript), Object.keys and dynamic property access."
native_way: "Typed annotations and reflection over declared members."
sensei_tip: "Java objects have no dynamic property bag: reflection walks what the class declares, with types the compiler checked."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| `@Column("hours") decorator` | `@Column("hours") with RUNTIME retention` |
| `Object.keys(obj)` | `getRecordComponents()` |
| `obj[name]` | `accessor.invoke(obj)` |
| `Reflect.construct(Cls, args)` | `constructor.newInstance(args)` |
