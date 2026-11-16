---
from: javascript
status: draft
concept: "What the CPU does with JavaScript"
from_way: "Ignoring that a JIT is speculating about your types."
native_way: "V8 interprets bytecode, then TurboFan compiles hot functions to machine code specialised for the types it saw; a type change triggers deoptimisation back to the interpreter."
sensei_tip: "Keep hot functions monomorphic (same object shapes and types); `node --trace-deopt` shows where speculation fails."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| a hot function | TurboFan machine code |
| changing an object's shape | a deoptimisation |
| `RangeError: Maximum call stack size exceeded` | the stack is full |
| typed arrays | contiguous, unboxed numbers |
| WebAssembly | a stack-machine bytecode close to the ISA |
