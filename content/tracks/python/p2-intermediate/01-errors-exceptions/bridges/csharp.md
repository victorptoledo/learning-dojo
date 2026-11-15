---
from: csharp
status: draft
concept: "Exceptions, inner exceptions and filters"
from_way: "Unchecked exceptions, catch with when filters, new Exception(msg, inner) for chaining, throw; to rethrow, AggregateException from tasks, and using for cleanup."
native_way: "Unchecked exceptions, except clauses by type (with an if inside for filters), raise ... from error, bare raise, ExceptionGroup with except*, and with for cleanup."
sensei_tip: "AggregateException.Flatten and Handle are what except* does natively: each clause takes the matching exceptions out of the group and the rest keep propagating."
---

| C# | Python |
| --- | --- |
| `throw new ArgumentException(msg)` | `raise ValueError(msg)` |
| `new PaymentException(msg, inner)` | `raise PaymentError(msg) from inner` |
| `throw;` | `raise` |
| `catch (HttpRequestException e) when (e.StatusCode == 503)` | `except HTTPError as e:` then `if e.status != 503: raise` |
| `AggregateException` | `ExceptionGroup` and `except*` |
| `KeyNotFoundException` | `KeyError` |
| `using var file = ...` | `with open(...) as file:` |
