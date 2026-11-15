---
from: javascript
status: draft
concept: "Errors, throw and rejected promises"
from_way: "throw new Error, try/catch around sync and await code, error.cause for chaining, instanceof checks on error classes, and finally for cleanup."
native_way: "Errors returned next to the result, checked with if err != nil, wrapped with %w, inspected with errors.Is and errors.As, and cleaned up with defer."
sensei_tip: "Go's (value, error) is close to the Result pattern some TypeScript code bases build by hand; the compiler forces you to receive the error, and linters flag ignored ones."
---

| JavaScript | Go |
| --- | --- |
| `throw new Error("not found")` | `return nil, errors.New("not found")` |
| `new Error("load", { cause: err })` | `fmt.Errorf("load: %w", err)` |
| `try { ... } catch (e) { ... }` | `if err != nil { ... }` |
| `e instanceof NotFoundError` | `errors.Is(err, ErrNotFound)` |
| `e instanceof ValidationError && e.field` | `errors.AsType[*ValidationError](err)` |
| `finally { conn.close() }` | `defer conn.Close()` |
| `AggregateError` | `errors.Join` |
| uncaught exception | `panic` (for bugs only) |
