---
from: javascript
status: draft
concept: "The same protocol, with types for what flows through it"
from_way: "for...of, spread, generators and iterator helpers, with nothing saying what a generator yields or returns."
native_way: "The same runtime, with Iterable<T>, Generator<T, TReturn, TNext> and IteratorObject<T> types, so helpers keep the element type and overloads narrow it."
sensei_tip: "Nothing changes at runtime. Type your parameters as Iterable<T> and annotate exported generators' return types; inference does the rest."
---

| JavaScript | TypeScript |
| --- | --- |
| `function* ids() { yield 1; }` | `function* ids(): Generator<number, void, undefined>` |
| `function sum(values)` | `function sum(values: Iterable<number>)` |
| `[Symbol.iterator]() { ... }` | The same, in a class that `implements Iterable<T>` |
| `filter(xs, isString)` returns anything | An overload with `(x: T) => x is S` returns `Generator<S>` |
