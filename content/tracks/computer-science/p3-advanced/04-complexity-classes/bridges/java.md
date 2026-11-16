---
from: java
status: draft
concept: "Hard problems from Java"
from_way: "Writing a brute-force search for a scheduling or routing problem."
native_way: "For NP-hard problems, Java has mature solvers: Google OR-Tools (CP-SAT, routing), Timefold (formerly OptaPlanner, local search), Choco (constraint programming) and SAT4J."
sensei_tip: "Model the problem and hand it to OR-Tools' CP-SAT with a time limit: it combines SAT, LP and search, and usually beats hand-written heuristics."
---

| In Java | Underneath |
| --- | --- |
| OR-Tools CP-SAT | constraint programming on a SAT core |
| Timefold Solver | local search and metaheuristics |
| SAT4J | a SAT solver in pure Java (used by Eclipse's dependency resolver) |
| `BigInteger.isProbablePrime` | primality: in P, in practice randomised |
| recursion with memoisation | exact for small n only |
