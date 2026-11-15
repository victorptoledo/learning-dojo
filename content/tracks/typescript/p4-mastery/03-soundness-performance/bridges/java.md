---
from: java
status: draft
concept: "Soundness and variance"
from_way: "Java's type system is mostly sound (with holes such as array covariance, which throws ArrayStoreException at runtime, and unchecked generic casts); use-site variance with ? extends and ? super."
native_way: "TypeScript is unsound by design, and nothing at runtime catches the mistake; variance is declaration-site, inferred from structure or stated with in and out."
sensei_tip: "Java's ArrayStoreException is a runtime safety net TypeScript does not have: `animals.push(cat)` on a Dog[] just corrupts the array. Use readonly arrays for anything you only read."
---

| Java | TypeScript |
| --- | --- |
| `List<? extends Animal>` (use-site) | `ReadonlyArray<Animal>` or `interface P<out T>` (declaration-site) |
| `List<? super Dog>` | `interface Consumer<in T>` |
| `ArrayStoreException` | No runtime check |
| `@SuppressWarnings("unchecked")` | `as` / `// @ts-expect-error` with a reason |
| Compiler profiling with `-Xlint`, build scans | `tsc --extendedDiagnostics`, `--generateTrace` |
