---
from: java
status: draft
concept: "Absence and failure"
from_way: "Returning null for not found, `if (x != null)` everywhere, checked exceptions wrapped in RuntimeException inside lambdas, and finally blocks that close streams."
native_way: "Optional return types consumed with map, flatMap and orElseThrow; specific unchecked exceptions with values in the message; try-with-resources."
sensei_tip: "`Optional.isPresent()` followed by `get()` is a null check in disguise: replace it with `map`, `ifPresent` or `orElseThrow`."
---

| Before | Now |
| --- | --- |
| `User u = find(id); if (u == null) throw ...` | `find(id).orElseThrow(() -> new UserNotFound(id))` |
| `if (opt.isPresent()) return opt.get().name(); return "?";` | `opt.map(User::name).orElse("?")` |
| `finally { if (in != null) in.close(); }` | `try (var in = ...) { ... }` |
| `catch (Exception e) {}` | Catch the specific type; never leave it empty |
| `catch (IOException ignored)` | `catch (IOException _)` |
