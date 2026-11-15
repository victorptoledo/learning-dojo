---
from: javascript
status: draft
concept: "Absence and failure"
from_way: "null, undefined, optional chaining and thrown errors or rejected promises."
native_way: "Optional with map and orElse, and typed exceptions."
sensei_tip: "a?.b ?? c becomes Optional.ofNullable(a).map(A::b).orElse(c). Exceptions in Java are typed, so catch (IOException e) catches only that family."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| null / undefined | Optional.empty() |
| a ?? b | opt.orElse(b) |
| a?.b | opt.map(A::b) |
| throw new Error() | throw new IllegalStateException() |
| try/catch(e) | try/catch (SpecificException e) |
