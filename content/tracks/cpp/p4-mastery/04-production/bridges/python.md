---
from: python
status: draft
concept: "C++ in production"
from_way: "pip and wheels, memory safety by the interpreter, and C extensions or pybind11 for native code."
native_way: "vcpkg or Conan, reproducible CMake builds, layered memory-safety checks, and pybind11 or a C interface to be called from Python."
sensei_tip: "When Python calls your C++ code, catch every exception at the boundary and let pybind11 or nanobind translate errors."
---

| Python | C++ |
| --- | --- |
| pip, wheels | vcpkg, Conan, packaged binaries |
| `pyproject.toml` | `CMakeLists.txt` + `CMakePresets.json` |
| interpreter is memory safe | ASan, UBSan, fuzzing, hardening |
| `ctypes` / C extensions | `extern "C"` API, pybind11, nanobind |
| hypothesis (property tests) | libFuzzer, AFL++ |
