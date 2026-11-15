---
from: go
status: draft
concept: "Absence and failure"
from_way: "Multiple returns with error values and nil for absence."
native_way: "Optional for absence and exceptions for failure."
sensei_tip: "Go's val, err := f() becomes a method that returns the value or throws. errors.Is/As become catch clauses on exception types, and %w wrapping is the cause argument of an exception."
---

| Go | Java 25 |
| --- | --- |
| return nil, ErrNotFound | return Optional.empty() or throw |
| if err != nil { return err } | let the exception propagate |
| fmt.Errorf("...: %w", err) | new RuntimeException("...", cause) |
| defer f.Close() | try-with-resources |
