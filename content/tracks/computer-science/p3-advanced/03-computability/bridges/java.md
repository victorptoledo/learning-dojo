---
from: java
status: draft
concept: "Undecidability and Java tooling"
from_way: "Expecting the compiler to catch every bug."
native_way: "javac's definite-assignment and reachability rules are deliberately conservative approximations: they reject some correct code (\"variable might not have been initialized\") because exact checking is undecidable."
sensei_tip: "When javac says a variable might not be initialized although you know it is, you are seeing Rice's theorem: restructure the code instead of fighting the analysis."
---

| In Java | Underneath |
| --- | --- |
| "might not have been initialized" | a sound, conservative analysis |
| "unreachable statement" | simple syntactic rules, not real reachability |
| NullAway, Checker Framework | sound-ish nullness with annotations |
| SpotBugs, Error Prone | useful, unsound bug finders |
| `ExecutorService` timeouts | the practical answer to non-halting |
