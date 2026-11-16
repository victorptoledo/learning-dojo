---
from: java
status: draft
concept: "Exceptions and error results"
from_way: "Checked and unchecked exceptions, try/catch/finally, Optional for missing values."
native_way: "Only unchecked exceptions, RAII instead of finally, noexcept for functions that cannot throw, and std::expected for expected failures."
sensei_tip: "There are no checked exceptions in C++: put expected failures in the type with std::expected, and keep exceptions for the rest."
---

| Java | C++ |
| --- | --- |
| `throws IOException` (checked) | no checked exceptions; `std::expected<T, E>` for declared failures |
| `catch (IOException e)` | `catch (const std::exception& e)` |
| `finally { close(); }` | a destructor (RAII) |
| `IllegalArgumentException` | `std::invalid_argument` |
| `Optional.empty()` | `std::nullopt` / `std::unexpected(err)` |
| throwing from a constructor | the same: the object never exists |
| `Error`/`OutOfMemoryError` | `std::bad_alloc`, often left uncaught |
