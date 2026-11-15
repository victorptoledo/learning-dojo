---
from: java
status: draft
concept: "Type checking before running"
from_way: "javac compiles to bytecode and refuses to produce it when types are wrong; types also exist at runtime (reflection, casts that throw ClassCastException)."
native_way: "tsc checks and then the types are erased; Node runs the stripped JavaScript even with type errors, and nothing checks types at runtime."
sensei_tip: "In Java a successful build means the types hold; in TypeScript a running program means nothing about types. Make `tsc` a required CI step, like compilation is in Maven."
---

| Java | TypeScript |
| --- | --- |
| `javac` / `mvn compile` | `tsc` (with `noEmit` when Node runs the `.ts` files) |
| `java Main` | `node main.ts` |
| `pom.xml` / `build.gradle` | `package.json` + `tsconfig.json` |
| Generics and types kept partly at runtime | All types erased |
| `ClassCastException` on a bad cast | A bad `as` cast is never checked at runtime |
