---
from: javascript
status: draft
concept: "Iterators and closures"
from_way: "Array methods map, filter and reduce, which build a new array at each step."
native_way: "Lazy iterator chains that build nothing until collected."
sensei_tip: "arr.filter(...).map(...) allocates twice in JavaScript. The Rust chain is fused into one loop by the compiler and often runs as fast as a hand-written for loop."
---

| JavaScript / TypeScript | Rust |
| --- | --- |
| arr.map(x => x * 2) | v.iter().map(\|x\| x * 2) |
| arr.filter(f) | .filter(f) |
| arr.reduce((a, b) => a + b, 0) | .fold(0, \|a, b\| a + b) or .sum() |
| arr.find(f) | .find(f) |
| arr.some(f) | .any(f) |
| x => x + 1 | \|x\| x + 1 |
