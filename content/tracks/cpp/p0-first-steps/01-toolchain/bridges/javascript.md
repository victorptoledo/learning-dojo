---
from: javascript
status: draft
concept: "Compiling and building"
from_way: "Node or the browser parse and JIT JavaScript at run time; bundlers and tsc transform sources; npm resolves packages."
native_way: "C++ goes through preprocessor, compiler and linker ahead of time into a native executable; nothing is resolved at run time."
sensei_tip: "Think of #include as a very literal copy-paste bundler step, and of the linker as the step that fails when an import has no implementation."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| `import { greet } from './greet.js'` | `#include "greet.hpp"` + `greet.cpp` in the target |
| `npm install`, `package.json` | vcpkg/Conan, `CMakeLists.txt` |
| `tsc --noEmit` type errors | compiler errors (types are always checked) |
| `node app.js` | `./app` (native machine code) |
| ESLint, Prettier | clang-tidy, clang-format |
| V8's GC | no GC: RAII and destructors |
