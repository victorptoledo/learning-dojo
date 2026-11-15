---
from: csharp
status: draft
concept: "Variance and nullable gaps"
from_way: "Generic variance with in and out on interfaces and delegates, array covariance with ArrayTypeMismatchException, nullable reference types as warnings with the ! operator."
native_way: "The same in and out keywords (optional, since variance is inferred), array covariance without any runtime check, and ! / as as unchecked escape hatches."
sensei_tip: "TypeScript copied in and out from C#, with the same meaning. The difference: in C# a bad array store throws; in TypeScript nothing stops it."
---

| C# | TypeScript |
| --- | --- |
| `IEnumerable<out T>` | `interface P<out T>` |
| `Action<in T>` | `interface C<in T>` / `(value: T) => void` |
| `ArrayTypeMismatchException` | No runtime check |
| `value!` (null-forgiving) | `value!` (non-null assertion) |
| Roslyn build performance reports | `--extendedDiagnostics`, `--generateTrace` |
