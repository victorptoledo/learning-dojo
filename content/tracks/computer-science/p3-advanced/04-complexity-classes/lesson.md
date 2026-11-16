# Complexity Classes: P, NP and Intractability

Computability asked what can be computed at all. Complexity asks what can be computed **with
reasonable resources**. The Data Structures & Algorithms track teaches you to analyse a single
algorithm in Big-O; this lesson is about **problems**: is there any fast algorithm for this, or are
we probably stuck with exponential time? Recognising the second case saves weeks of searching for
a clever algorithm that very likely does not exist.

## Polynomial time as "feasible"

Complexity theory works with **decision problems** (yes/no answers: "is there a route shorter than
k?") measured by the input size n. The working definition of efficient is **polynomial time**:
O(nᵏ) for some constant k.

| n | n² | n³ | 2ⁿ |
| --- | --- | --- | --- |
| 20 | 400 | 8,000 | 1,048,576 |
| 50 | 2,500 | 125,000 | 1.1 × 10¹⁵ |
| 100 | 10,000 | 10⁶ | 1.3 × 10³⁰ |

A 2ⁿ algorithm at a billion steps per second needs 35 years for n = 60. Polynomials compose
(a polynomial algorithm calling a polynomial subroutine is polynomial) and the class does not
depend on the machine model, which is why the line is drawn there.

## P and NP

- **P**: problems that can be **solved** in polynomial time. Sorting, shortest paths, maximum
  matching, primality testing (since 2002), linear programming.
- **NP**: problems whose yes-answers can be **checked** in polynomial time, given a certificate
  (a proposed solution). "Is there a route through all cities shorter than k?": hand me the route
  and I check it quickly. NP stands for *nondeterministic polynomial*: a machine that could guess
  the certificate would solve it in polynomial time.

P ⊆ NP: if you can solve it, you can check it (ignore the certificate and solve). Whether **P =
NP**, whether every problem whose solutions are easy to check is also easy to solve, is the most
famous open question in computer science and a Clay Millennium Prize problem. Almost everyone
believes P ≠ NP.

## Reductions and NP-completeness

A **polynomial reduction** from A to B transforms any instance of A into an instance of B with
the same answer, in polynomial time. Then B is at least as hard as A: a fast algorithm for B gives
one for A.

- A problem is **NP-hard** if every problem in NP reduces to it.
- It is **NP-complete** if it is NP-hard **and** in NP: the hardest problems in NP.
- **Cook-Levin theorem** (1971): **SAT**, deciding whether a Boolean formula has a satisfying
  assignment, is NP-complete. Any polynomial-time verifier can be encoded as a circuit, and a
  circuit as a formula.
- Karp (1972) then reduced SAT to 21 other problems, and thousands followed. A fast algorithm for
  **any** NP-complete problem would give fast algorithms for **all** of NP, and prove P = NP.

Problems you will meet in disguise:

| NP-complete problem | Shows up as |
| --- | --- |
| SAT, 3-SAT | dependency resolution with conflicts (package managers), configuration, hardware verification |
| Graph colouring (k ≥ 3) | exam timetables, register allocation, frequency assignment |
| Travelling salesman (decision) | delivery routes, drilling circuit boards |
| Knapsack, subset sum | budgeting, packing, cutting stock |
| Bin packing | placing containers on machines, VMs on hosts |
| Vertex cover, clique, independent set | network monitoring, social network analysis |
| Hamiltonian path | DNA sequencing models, puzzles |
| Scheduling with constraints | job shop scheduling, sports fixtures |

Small changes can cross the line: shortest path is in P, longest simple path is NP-hard; 2-SAT
and 2-colouring are in P, 3-SAT and 3-colouring are NP-complete; an Euler circuit (each edge once)
is easy, a Hamiltonian cycle (each vertex once) is NP-complete.

## Beyond NP

- **co-NP**: problems whose no-answers have short certificates ("this formula is unsatisfiable").
- **PSPACE**: solvable with polynomial memory, possibly exponential time; deciding who wins
  generalised games, or whether two regular expressions are equivalent.
- **EXPTIME**: exponential time; generalised chess and Go on n × n boards are provably not in P.
- **Undecidable** problems, from the previous lesson, sit outside all of these.
- P ⊆ NP ⊆ PSPACE ⊆ EXPTIME, and P ≠ EXPTIME is known; most of the other inclusions are open.

Some problems are in NP but not believed NP-complete: **integer factoring** and **graph
isomorphism**. RSA's security rests on factoring being hard in practice, and quantum computers
(Shor's algorithm) would break it without saying anything about P versus NP.

## How engineers cope with NP-hard problems

NP-hardness is about the **worst case** of **exact** solutions on **all** inputs. Relax any of
those three:

- **Small or structured inputs**: exponential is fine for n = 20 (bitmask dynamic programming,
  backtracking with pruning, branch and bound). Trees, planar graphs, or bounded parameters often
  admit polynomial algorithms (*fixed-parameter tractability*).
- **Approximation** with a guarantee: a vertex cover at most twice the optimum, Christofides'
  algorithm for metric TSP within 1.5×, a knapsack within (1 − ε) in polynomial time.
- **Heuristics** without a guarantee: greedy, local search, simulated annealing, genetic
  algorithms. Usually very good, sometimes bad.
- **Solvers**: modern SAT solvers (CDCL), SMT solvers (Z3), constraint programming and integer
  linear programming (Gurobi, CP-SAT, HiGHS) solve industrial instances with millions of variables.
  The trick is to **reduce your problem to theirs**, exactly what reductions are for.

And hardness can be useful: cryptography **needs** problems that are easy to check and hard to
solve.

## Summary

- Polynomial time is the working definition of feasible; exponential time is not, beyond small n.
- P is solvable fast; NP is checkable fast; P versus NP is open, and most believe they differ.
- Reductions compare problems; NP-complete problems are the hardest in NP, starting from SAT.
- Many everyday problems (scheduling, packing, routing, dependency resolution) are NP-complete in
  disguise.
- Cope with exact algorithms on small or structured inputs, approximation, heuristics, or a
  solver.

## Further reading

- Sipser, *Introduction to the Theory of Computation*, 3rd ed., chapter 7 "Time Complexity"
- Garey and Johnson, *Computers and Intractability: A Guide to the Theory of NP-Completeness*
- Kleinberg and Tardos, *Algorithm Design*, chapters 8 "NP and Computational Intractability" and 11 "Approximation Algorithms"
- Cormen, Leiserson, Rivest and Stein, *Introduction to Algorithms*, 4th ed., chapter 34 "NP-Completeness"
- Scott Aaronson, "P =? NP" (survey): <https://www.scottaaronson.com/papers/pnp.pdf>
- Clay Mathematics Institute, "P vs NP": <https://www.claymath.org/millennium/p-vs-np/>
