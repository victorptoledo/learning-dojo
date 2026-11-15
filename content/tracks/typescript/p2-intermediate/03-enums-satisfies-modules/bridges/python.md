---
from: python
status: draft
concept: "Enums, Final constants and modules"
from_way: "enum.Enum and StrEnum classes, Literal types, Final constants; every file is a module, and imports are resolved at runtime from sys.path."
native_way: "Literal unions and as const objects instead of enum; satisfies checks a constant without widening it; ES modules with explicit file extensions and import type."
sensei_tip: "Python's StrEnum is closest to an as const object of strings. TypeScript has no __all__: only what you export is visible, and package.json exports plays the role of a curated public API."
---

| Python | TypeScript |
| --- | --- |
| `class Color(StrEnum): RED = "red"` | `const Color = { Red: "red" } as const` |
| `Literal["red", "green"]` | `"red" \| "green"` |
| `ROUTES: Final = {...}` | `const ROUTES = {...} as const` |
| `from .money import format` | `import { format } from "./money.ts"` |
| `if TYPE_CHECKING: from x import T` | `import type { T } from "./x.ts"` |
| `__all__` | Explicit `export`s and `package.json` `exports` |
