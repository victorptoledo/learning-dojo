---
from: go
status: draft
concept: "Errors as values versus exceptions"
from_way: "Functions return (value, error), callers check if err != nil, wrap with fmt.Errorf(\"...: %w\", err), inspect with errors.Is and errors.As, and combine with errors.Join."
native_way: "Functions raise, callers handle with except only where they can act, raise ... from error wraps, except SomeError matches by type, and ExceptionGroup joins several."
sensei_tip: "The if err != nil at every level disappears: in Python you let exceptions travel up and catch them once at the boundary. Wrap with from exactly where Go would use %w."
---

| Go | Python |
| --- | --- |
| `return nil, fmt.Errorf("load: %w", err)` | `raise LoadError("load") from err` |
| `if err != nil { return err }` | nothing: the exception propagates |
| `errors.Is(err, fs.ErrNotExist)` | `except FileNotFoundError:` |
| `var pe *PathError; errors.As(err, &pe)` | `except OSError as pe:` |
| `errors.Join(e1, e2)` | `ExceptionGroup("...", [e1, e2])` |
| `var ErrNotFound = errors.New(...)` | `class NotFoundError(LookupError): ...` |
| `defer f.Close()` | `with open(...) as f:` |
| `panic` / `recover` | an uncaught exception / a catch-all at the boundary |
