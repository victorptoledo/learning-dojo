---
from: go
status: draft
concept: "Clear is better than clever"
from_way: "Small functions with doc comments, explicit errors for every failure, and no hidden behaviour."
native_way: "A prompt that is plain and explicit: concrete limits, the reasons for rules, and a defined value for 'I cannot do this', like a sentinel error."
sensei_tip: "Treat NOT_AN_ORDER like io.EOF: a documented, expected result that the caller checks for, not an accident."
---

| Go | Prompt concept |
| --- | --- |
| a doc comment on an exported func | task and output format |
| a sentinel error such as `io.EOF` | the explicit exit value |
| table-driven tests | success criteria with example inputs |
| "clear is better than clever" | specific instructions over clever wording |
| a comment explaining why | the reason behind a rule |
