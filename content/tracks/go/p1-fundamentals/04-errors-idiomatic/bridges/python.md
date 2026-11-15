---
from: python
status: draft
concept: "Exceptions and error handling"
from_way: "raise and try/except, exception class hierarchies, raise ... from error for chaining, with statements for cleanup and EAFP style."
native_way: "Errors returned as values and checked at once, %w for chaining, errors.Is and errors.As to inspect, defer for cleanup, and panic only for bugs."
sensei_tip: "raise New(...) from error maps to fmt.Errorf(\"...: %w\", err). except SomeError maps to errors.Is or errors.As. The difference is that the check is always right there in the code."
---

| Python | Go |
| --- | --- |
| `raise ValueError("bad qty")` | `return fmt.Errorf("bad qty %d", qty)` |
| `class NotFound(Exception)` | `var ErrNotFound = errors.New("not found")` |
| `class ValidationError(Exception)` with fields | `type ValidationError struct{ Field string }` + `Error()` |
| `raise Wrapped("x") from e` | `fmt.Errorf("x: %w", err)` |
| `except NotFound:` | `if errors.Is(err, ErrNotFound)` |
| `except ValidationError as e:` | `if ve, ok := errors.AsType[*ValidationError](err); ok` |
| `with open(p) as f:` | `f, err := os.Open(p); defer f.Close()` |
| `ExceptionGroup` | `errors.Join` |
| `assert` for invariants | `panic` |
