---
from: csharp
status: draft
concept: "Absence and failure"
from_way: "Nullable reference types and exceptions, all unchecked."
native_way: "Optional for return values and the checked/unchecked split."
sensei_tip: "C# has no checked exceptions, so the Java ones (IOException) feel noisy at first. Wrap them into an unchecked exception at module boundaries, keeping the cause."
---

| C# | Java 25 |
| --- | --- |
| string? | Optional<String> (return values only) |
| x ?? y | opt.orElse(y) |
| ArgumentException | IllegalArgumentException |
| InvalidOperationException | IllegalStateException |
| using (var r = ...) | try-with-resources |
