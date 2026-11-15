---
from: javascript
status: draft
concept: "How much the types can be trusted"
from_way: "No static types, so every value must be checked or trusted by convention."
native_way: "Static types that are trustworthy where you avoid the holes: any, assertions, unchecked indexing and bivariant methods."
sensei_tip: "TypeScript's guarantees are only as strong as the escape hatches you allow. Treat any, as and ! the way you would treat eval: rare, commented, reviewed."
---

| JavaScript | TypeScript |
| --- | --- |
| `arr[10]` is `undefined` at runtime | `T` unless `noUncheckedIndexedAccess` |
| `{ theme: undefined }` vs `{}` | The same type unless `exactOptionalPropertyTypes` |
| Duck-typed callbacks | Property-style callbacks for strict checks |
| No build cost for types | Type-check time to measure and manage |
