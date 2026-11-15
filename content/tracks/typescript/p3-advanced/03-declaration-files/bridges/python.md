---
from: python
status: draft
concept: "Stub files"
from_way: "Inline annotations, .pyi stub files for untyped packages, typeshed and types-* packages on PyPI, py.typed markers."
native_way: ".d.ts files are TypeScript's stubs: bundled with packages or published as @types/* from DefinitelyTyped, the equivalent of typeshed."
sensei_tip: "If you have written a .pyi stub, you have written a .d.ts. @types/express is to Express what types-requests is to requests."
---

| Python | TypeScript |
| --- | --- |
| `requests.pyi` stub | `legacy-lib.d.ts` |
| `types-requests` (typeshed) | `@types/express` (DefinitelyTyped) |
| `py.typed` marker | `"types"` in `package.json` `exports` |
| `def f(x: int) -> str: ...` in a stub | `export declare function f(x: number): string;` |
| `stubgen` | `tsc --declaration` |
