# C++ in Production: Builds, Dependencies, Safety and Interop

Writing correct C++ in a kata is one thing; shipping it for years is another. Production C++
is about reproducible builds, a dependency strategy, a wall of automated checks against
memory-safety bugs, hardened builds, stable interfaces and clean boundaries with C and other
languages.

## Builds that everyone can reproduce

- **CMake presets** (`CMakePresets.json`) pin the generator, the compiler, the build type and
  the cache variables, so `cmake --preset release` means the same on every machine and in CI.
- Separate **Debug** (assertions, sanitizers), **RelWithDebInfo** (optimised, with symbols:
  what you profile and often what you ship) and **Release**.
- Turn warnings into errors in CI (`-Werror` / `/WX`) once the code is clean, with a
  deliberate list of warnings.
- Cache builds (ccache, sccache) and keep headers lean: include what you use, forward-declare,
  and consider **modules** (`import std;`) as tool support matures; they cut build times
  substantially.

## Dependencies

C++ has no single package manager. The two most used:

| Tool | Model |
| --- | --- |
| **vcpkg** (Microsoft) | a curated registry, manifest file `vcpkg.json`, builds from source, integrates with CMake through a toolchain file |
| **Conan** | recipes and binary packages, `conanfile.txt`/`.py`, profiles per compiler and platform |

Pin versions (a vcpkg baseline, Conan lockfiles), build dependencies with the same compiler
flags and standard library as your code, and prefer fewer, well-maintained libraries. CMake's
`FetchContent` (used by the dojo's kata CMakeLists for GoogleTest) is fine for small
dependencies and tests.

## The safety wall

Memory-safety bugs (use after free, buffer overflows, data races) are the main source of
security vulnerabilities in C and C++ code; Microsoft and Google have each reported about 70%
of their severe security bugs coming from them. Governments now ask for a memory-safety
roadmap. In C++ you answer with layers:

1. **Modern style**: RAII, containers, `std::span`, `std::string_view` used carefully, no
   naked `new`, no C casts, no pointer arithmetic. The Core Guidelines and their tooling
   (clang-tidy `cppcoreguidelines-*`) encode it.
2. **Sanitizers in CI**: ASan + UBSan on every test run, TSan on concurrency tests, MSan
   (Clang) for uninitialised reads.
3. **Static analysis**: clang-tidy, the compilers' `-fanalyzer` (GCC) or `/analyze` (MSVC),
   and commercial analysers for larger codebases.
4. **Fuzzing**: libFuzzer or AFL++ feed random inputs to parsers and decoders under ASan;
   most of the bugs they find are memory errors.
5. **Hardened builds**: standard library assertions in release (`-D_GLIBCXX_ASSERTIONS`,
   libc++'s hardening modes; C++26 standardises a "hardened" library), `-D_FORTIFY_SOURCE=3`,
   `-fstack-protector-strong`, `-fstack-clash-protection`, control-flow protection
   (`-fcf-protection`), position-independent executables and full RELRO. The OpenSSF
   compiler options guide lists them.

C++26 also brings **contracts** (preconditions and postconditions checked at run time) and
makes reading uninitialised variables "erroneous behaviour" with a defined value instead of
UB.

## Interfaces and ABI

An **ABI** (application binary interface) is how compiled code agrees on layout and calling
conventions. C++'s ABI is fragile: changing a class's private members changes its layout and
breaks code compiled against the old header. For libraries that ship binaries:

- Hide implementation with the **pimpl** idiom (a `std::unique_ptr<Impl>` member) so private
  changes do not change the layout.
- Do not pass standard library types across a boundary built with a different compiler or
  standard library.
- Version your shared libraries (sonames) and use symbol visibility (`-fvisibility=hidden`
  plus explicit exports).

## Interop with C and other languages

C is the lingua franca: every language can call a C function. Expose C++ through a C
interface:

```cpp
extern "C" {
struct Engine;                                   // opaque handle
Engine* engine_create(const char* config_path);  // returns nullptr on failure
int engine_run(Engine* engine, const char* input, char* out, size_t out_size);
void engine_destroy(Engine* engine);
}
```

- No exceptions may cross the boundary: catch everything inside and return an error code.
- Ownership rules must be explicit (who frees what, with which function).
- Higher-level bindings exist: pybind11 or nanobind for Python, JNI or the Foreign Function
  and Memory API (Java 22+) for Java, P/Invoke for C#, cgo for Go, N-API for Node.js, and
  `cxx` or `bindgen` for Rust.

## Operating it

- Ship symbols separately and keep crash reporting (minidumps, `addr2line`, Breakpad or
  Crashpad) so a crash in production has a stack trace.
- Structured logging, metrics and tracing with libraries such as spdlog and OpenTelemetry C++.
- Treat warnings, sanitizer reports and fuzzer findings as release blockers.

## Summary

- Reproducible builds with CMake presets, pinned dependencies (vcpkg, Conan) and CI.
- Layered memory safety: modern style, sanitizers, static analysis, fuzzing, hardened builds.
- Mind the ABI when you ship binaries; pimpl and C interfaces keep boundaries stable.
- Expose C++ to other languages through C, with no exceptions crossing.

## Further reading

- CMake, cmake-presets(7): <https://cmake.org/cmake/help/latest/manual/cmake-presets.7.html>
- vcpkg documentation: <https://learn.microsoft.com/vcpkg/>
- Conan documentation: <https://docs.conan.io/>
- OpenSSF, Compiler Options Hardening Guide for C and C++: <https://best.openssf.org/Compiler-Hardening-Guides/Compiler-Options-Hardening-Guide-for-C-and-C++.html>
- LLVM, libFuzzer: <https://llvm.org/docs/LibFuzzer.html>
- C++ Core Guidelines, I: Interfaces and the GSL: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-interfaces>
- Bjarne Stroustrup, "A call to action: think seriously about safety" (P2739) and safety profiles (P3081): <https://wg21.link/p3081>
- John Lakos, *Large-Scale C++, Volume I* (Addison-Wesley, 2019)
