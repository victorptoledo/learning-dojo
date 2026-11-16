---
from: python
status: draft
concept: "Compiling and building"
from_way: "Python compiles to bytecode on import and the interpreter finds modules on sys.path at run time; pip installs packages."
native_way: "C++ is compiled ahead of time to machine code: headers are pasted in by the preprocessor, each .cpp is compiled alone, and the linker resolves every call."
sensei_tip: "Errors you saw at run time in Python (a misspelled function) show up at build time in C++; read the first compiler error, not the last."
---

| Python | C++ |
| --- | --- |
| `import mymodule` | `#include "mymodule.hpp"` plus linking `mymodule.cpp` |
| `python app.py` | `cmake --build build` then `./build/app` |
| `NameError` at run time | `'x' was not declared in this scope` at compile time |
| `pip install`, `pyproject.toml` | vcpkg or Conan, `CMakeLists.txt` |
| one interpreter, any OS | one binary per OS and CPU |
| ruff, mypy | warnings, clang-tidy, sanitizers |
