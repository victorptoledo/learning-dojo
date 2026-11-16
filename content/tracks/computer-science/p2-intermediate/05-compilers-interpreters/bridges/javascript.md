---
from: javascript
status: draft
concept: "JavaScript and TypeScript compilers"
from_way: "Thinking TypeScript runs."
native_way: "tsc (and esbuild, swc) parse TypeScript, check types and emit JavaScript; V8 parses it again, compiles to Ignition bytecode for a register machine and JIT-compiles hot code with Sparkplug, Maglev and TurboFan."
sensei_tip: "AST Explorer (astexplorer.net) and `node --print-bytecode` show both halves: the tree tools work on, and the bytecode V8 runs."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| Babel, ESLint, Prettier | all work on an AST |
| `tsc` | type check + emit JavaScript |
| Ignition | a register-based bytecode interpreter |
| TurboFan | speculative optimising JIT |
| `node --print-bytecode` | the bytecode of your functions |
