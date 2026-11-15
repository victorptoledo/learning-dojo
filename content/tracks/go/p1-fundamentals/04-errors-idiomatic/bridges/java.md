---
from: java
status: draft
concept: "Exceptions and error handling"
from_way: "Checked and unchecked exceptions, try/catch/finally, exception hierarchies, new ServiceException(msg, cause) for chaining and try-with-resources for cleanup."
native_way: "Functions return (value, error), callers check if err != nil, fmt.Errorf with %w chains causes, errors.Is and errors.As replace catch clauses, and defer does the cleanup."
sensei_tip: "Think of %w as the cause argument of a Java exception constructor, and errors.As as a catch clause you can run anywhere on the chain."
---

| Java | Go |
| --- | --- |
| `throws IOException` | `func Load() (Config, error)` |
| `throw new NotFoundException(id)` | `return User{}, fmt.Errorf("find %s: %w", id, ErrNotFound)` |
| `catch (NotFoundException e)` | `if errors.Is(err, ErrNotFound)` |
| `catch (ValidationException e) { e.getField() }` | `if ve, ok := errors.AsType[*ValidationError](err); ok { ve.Field }` |
| `new ServiceException("save", cause)` | `fmt.Errorf("save: %w", err)` |
| `try (var in = open()) { ... }` | `in, err := open(); ...; defer in.Close()` |
| `finally` | `defer` |
| `e.addSuppressed(other)` | `errors.Join(err, other)` |
| `RuntimeException` for bugs | `panic` |
