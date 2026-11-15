---
from: csharp
status: draft
concept: "Generics with constraints"
from_way: "Reified generics with where T : class, new(), IComparable<T> constraints; variance with in and out on interfaces."
native_way: "Erased generics with extends constraints, structural constraints such as { length: number }, keyof and indexed access types."
sensei_tip: "Constraints are structural: instead of `where T : IHasLength`, write `T extends { length: number }`. There is no `new T()` because T does not exist at runtime."
---

| C# | TypeScript |
| --- | --- |
| `T First<T>(IEnumerable<T> items)` | `function first<T>(items: Iterable<T>)` |
| `where T : IComparable<T>` | `T extends { compareTo(o: T): number }` |
| `default(T)` | No equivalent: return `undefined` explicitly |
| `new T()` | Pass a factory `() => T` |
| `nameof(User.Name)` | `keyof User` |
