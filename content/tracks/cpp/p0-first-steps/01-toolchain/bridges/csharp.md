---
from: csharp
status: draft
concept: "Compiling and building"
from_way: "dotnet build compiles projects to IL assemblies, the CLR JITs them, NuGet provides packages, and references are resolved by assembly."
native_way: "C++ compiles each .cpp separately to native object files and links them; headers carry declarations across files; CMake plays the role of the .csproj."
sensei_tip: "A header is not an assembly reference: it only promises that a definition exists somewhere in the link."
---

| C# | C++ |
| --- | --- |
| `.csproj` + `dotnet build` | `CMakeLists.txt` + `cmake --build` |
| `using MyLib;` | `#include "mylib.hpp"` and `target_link_libraries` |
| NuGet | vcpkg (Microsoft) or Conan |
| IL + JIT, GC | native code, deterministic destructors |
| Roslyn analyzers | warnings, clang-tidy, MSVC `/analyze` |
| one file per class by convention | header + source per component |
