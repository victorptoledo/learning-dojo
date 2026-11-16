---
from: go
status: draft
concept: "Hard problems from Go"
from_way: "Assuming go modules' version selection is hard because it is in other ecosystems."
native_way: "Go modules use Minimal Version Selection precisely to avoid the NP-complete general dependency resolution problem; for real optimisation, Go has gophersat (SAT) and OR-Tools bindings."
sensei_tip: "Notice the design lesson of MVS: restricting the problem (no upper bounds, pick the minimum) turned an NP-complete problem into a polynomial one."
---

| In Go | Underneath |
| --- | --- |
| Minimal Version Selection | a restricted, polynomial version problem |
| `gophersat` | a SAT solver in Go |
| OR-Tools (cgo) | CP-SAT |
| `math/big.Int.ProbablyPrime` | fast primality |
| bitmask DP over 2ⁿ states | exact for small n |
