---
from: java
status: draft
concept: "Enums, constants and packages"
from_way: "enum is a full class type with values(), methods and EnumMap; packages and the module system (module-info.java) control visibility."
native_way: "Prefer unions of string literals and as const objects to enum; satisfies checks constant tables; files are modules with explicit import and export, and package.json exports controls what is public."
sensei_tip: "A Java enum with fields maps best to an as const object plus a derived union. Exhaustiveness on switch comes from the union and never, not from the enum."
---

| Java | TypeScript |
| --- | --- |
| `enum Level { DEBUG, INFO }` | `type Level = "debug" \| "info"` |
| `Level.values()` | `const LEVELS = ["debug", "info"] as const` |
| `EnumMap<Status, List<Status>>` | `{ ... } as const satisfies Record<Status, readonly Status[]>` |
| `import com.acme.Money;` | `import { Money } from "./money.ts"` |
| `module-info.java` `exports com.acme.api` | `package.json` `"exports"` |
| `public` / package-private | `export` / not exported |
