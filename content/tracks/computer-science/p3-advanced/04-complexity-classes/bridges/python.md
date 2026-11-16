---
from: python
status: draft
concept: "Hard problems from Python"
from_way: "Using itertools.permutations to solve a travelling salesman problem."
native_way: "Python has OR-Tools, PuLP and Pyomo (linear and integer programming), python-sat and z3-solver; permutations of 15 cities is already 1.3 trillion routes."
sensei_tip: "Reach for `ortools` (CP-SAT, routing) or `pulp` before writing a search; reduce your problem to theirs."
---

| In Python | Underneath |
| --- | --- |
| `itertools.permutations(cities)` | n! candidates |
| `ortools.sat.python.cp_model` | CP-SAT |
| `pulp` / `pyomo` | integer linear programming |
| `z3` | an SMT solver |
| `networkx.algorithms.approximation` | approximation algorithms |
