---
from: csharp
status: draft
concept: "Exceptions and error handling"
from_way: "throw and try/catch/finally, exception filters with when, InnerException for chaining, using statements for IDisposable, and the TryParse pattern for expected failures."
native_way: "Every fallible function follows the TryParse idea: it returns a value and an error. %w chains causes, errors.Is and errors.As filter, and defer replaces using."
sensei_tip: "If you liked int.TryParse(s, out var n), you already know idiomatic Go: n, err := strconv.Atoi(s) is the same contract for every API."
---

| C# | Go |
| --- | --- |
| `throw new KeyNotFoundException()` | `return fmt.Errorf("get %q: %w", key, ErrNotFound)` |
| `catch (KeyNotFoundException)` | `if errors.Is(err, ErrNotFound)` |
| `catch (ValidationException e) when (e.Field == "email")` | `if ve, ok := errors.AsType[*ValidationError](err); ok && ve.Field == "email"` |
| `new AppException("save", inner)` | `fmt.Errorf("save: %w", err)` |
| `using var f = File.OpenRead(p);` | `f, err := os.Open(p); defer f.Close()` |
| `AggregateException` | `errors.Join` |
| `int.TryParse(s, out var n)` | `n, err := strconv.Atoi(s)` |
| `Environment.FailFast` / bugs | `panic` |
