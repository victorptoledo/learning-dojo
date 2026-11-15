---
from: javascript
status: draft
concept: "Composition, coupling and cohesion"
from_way: "Prototype chains or `class ... extends` three levels deep, and modules that share a mutable exported object."
native_way: "Factory functions that combine small behaviours, objects passed in as dependencies, and immutable data passed between modules."
sensei_tip: "React moved from mixins and class inheritance to hooks precisely to favour composition: each hook is a small reusable behaviour you combine in a function."
---

| JavaScript | Idea |
| --- | --- |
| `{ ...defaults, ...overrides }` and factory functions | composition |
| `class A extends B extends C` | inheritance chains (avoid) |
| a mutable object exported from a module | common coupling |
| passing `{ logger, db }` | explicit dependencies |
| `Object.freeze` data | lower coupling through immutability |
