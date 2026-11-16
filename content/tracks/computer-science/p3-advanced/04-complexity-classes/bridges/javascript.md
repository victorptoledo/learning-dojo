---
from: javascript
status: draft
concept: "Hard problems from JavaScript"
from_way: "Running an exponential search in a browser tab."
native_way: "JavaScript has javascript-lp-solver, logic-solver (MiniSat compiled to JavaScript) and WebAssembly builds of real solvers such as Z3 and HiGHS."
sensei_tip: "Run heavy search in a Web Worker or on the server with a time budget, and return the best solution found so far."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| brute force over subsets | 2ⁿ: fine for n ≈ 20 |
| `logic-solver` | SAT in JavaScript |
| `z3-solver` (Wasm) | SMT in the browser or Node |
| `highs` (Wasm) | linear and integer programming |
| Web Worker | keeps the UI responsive during search |
