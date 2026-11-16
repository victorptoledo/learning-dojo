---
from: csharp
status: draft
concept: "C++ in production"
from_way: "NuGet, stable IL across runtimes, memory safety by the CLR, P/Invoke and C++/CLI for native code."
native_way: "vcpkg (also by Microsoft) or Conan, fragile ABI, memory safety through tooling, and C interfaces for P/Invoke."
sensei_tip: "P/Invoke wants an `extern \"C\"` function with plain types: give it one, and keep exceptions on the C++ side."
---

| C# | C++ |
| --- | --- |
| NuGet | vcpkg, Conan |
| `.csproj` configurations | CMake presets |
| P/Invoke `[DllImport]` | `extern "C"` exports |
| CLR memory safety | sanitizers, fuzzing, hardening, MSVC `/analyze` |
| strong-named assemblies | sonames, symbol visibility |
