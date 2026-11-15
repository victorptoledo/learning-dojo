---
from: python
status: draft
concept: "Absence and failure"
from_way: "None for absence and exceptions for everything that fails."
native_way: "Optional for an absent result, unchecked exceptions for bugs and bad input, checked exceptions where callers must react."
sensei_tip: "Java adds checked exceptions, which a caller must catch or declare. Use Optional only as a return type, never for fields or parameters."
---

| Python | Java 25 |
| --- | --- |
| return None | return Optional.empty() |
| x if x is not None else d | opt.orElse(d) |
| raise ValueError(...) | throw new IllegalArgumentException(...) |
| try/except/finally | try/catch/finally |
| with open(...) as f: | try (var f = Files.newBufferedReader(p)) |
