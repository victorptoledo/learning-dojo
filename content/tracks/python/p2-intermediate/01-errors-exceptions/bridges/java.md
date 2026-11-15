---
from: java
status: draft
concept: "Exceptions without checked exceptions"
from_way: "Checked exceptions declared with throws, unchecked RuntimeExceptions, try-with-resources, new WrapperException(\"...\", cause) for chaining and addSuppressed for secondary errors."
native_way: "Every exception is unchecked, raise ... from error chains, with statements replace try-with-resources, add_note adds context and ExceptionGroup carries several errors."
sensei_tip: "Without throws clauses, documentation and a clear hierarchy per module are what tell callers what to catch. Write the Raises section of your docstrings."
---

| Java | Python |
| --- | --- |
| `throw new IllegalArgumentException(msg)` | `raise ValueError(msg)` |
| `catch (IOException \| SQLException e)` | `except (OSError, sqlite3.Error) as e:` |
| `new ServiceException("x", cause)` | `raise ServiceException("x") from cause` |
| `throw e;` to rethrow inside catch | bare `raise` |
| `NoSuchElementException` / `IndexOutOfBounds` | `KeyError` / `IndexError` (both `LookupError`) |
| `e.addSuppressed(other)` | `e.add_note(...)` or an `ExceptionGroup` |
| `throws PaymentException` | a `Raises:` section in the docstring |
| `finally` | `finally` (and `else` for the success path) |
