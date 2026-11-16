# The Toolchain and the Build Model

C++ is a compiled language with no single official tool. Your code goes through a
**preprocessor**, a **compiler** and a **linker**, usually driven by **CMake**, and the
result is a native executable with no virtual machine and no garbage collector. Knowing what
each step does explains most of the error messages you will meet in your first weeks.

## Why C++ still matters

C++ gives you **zero-overhead abstractions**: a `std::vector` or a lambda costs what the
hand-written code would cost, and you pay nothing for features you do not use. You control
memory layout and lifetime, so it runs browsers, game engines, databases, trading systems,
compilers, embedded firmware and the runtimes of other languages (the JVM, V8 and CPython
are written in C or C++). The price is responsibility: the compiler trusts you, and some
mistakes are **undefined behaviour** instead of an exception.

## Standards and compilers

The language is an ISO standard revised every three years: C++11 (the start of "modern
C++"), 14, 17, 20 (concepts, ranges, coroutines, modules), **C++23** (this track: `std::print`,
`std::expected`, `std::ranges::to`, `std::generator`) and C++26. Three compilers matter:

| Compiler | Where | Standard library |
| --- | --- | --- |
| **GCC** (`g++`) | Linux, the dojo's checks (GCC 16) | libstdc++ |
| **Clang** (`clang++`) | macOS (Apple Clang), Linux, tools like clang-tidy | libc++ or libstdc++ |
| **MSVC** (`cl.exe`) | Windows, Visual Studio | Microsoft STL |

Pick the standard explicitly: `g++ -std=c++23`, or `set(CMAKE_CXX_STANDARD 23)` in CMake.
Support is uneven at the edges; [cppreference's compiler support page](https://en.cppreference.com/w/cpp/compiler_support)
lists which compiler has which feature.

## From source to executable

```text
greet.hpp ─┐
greet.cpp ─┼─ preprocessor ─ compiler ─ greet.o ─┐
main.cpp  ─┴─ preprocessor ─ compiler ─ main.o  ─┴─ linker ─ app
```

1. The **preprocessor** handles lines starting with `#`. `#include "greet.hpp"` pastes the
   header's text in place; `#define` and `#if` do text substitution and conditional code.
2. The **compiler** turns each `.cpp` file, now one big **translation unit**, into an object
   file (`.o` or `.obj`) of machine code. It compiles every translation unit on its own, so it
   only knows what that unit declares.
3. The **linker** joins the object files and libraries, matching each use of a function to
   its one definition.

```bash
g++ -std=c++23 -Wall -Wextra -c greet.cpp      # greet.o
g++ -std=c++23 -Wall -Wextra -c main.cpp       # main.o
g++ greet.o main.o -o app                      # link
./app
```

## Headers and the one definition rule

A **declaration** says that something exists; a **definition** creates it. Headers hold
declarations so every translation unit can use them; one `.cpp` holds the definitions:

```cpp
// greet.hpp
#pragma once                          // include this file once per translation unit
#include <string>
#include <string_view>

namespace dojo {
std::string greet(std::string_view name);   // declaration
}
```

```cpp
// greet.cpp
#include "greet.hpp"
#include <format>

namespace dojo {
std::string greet(std::string_view name) {   // definition
    return std::format("Hello, {}!", name);
}
}
```

The **one definition rule (ODR)** says each function or variable has exactly one
definition in the whole program (inline functions, templates and classes may be defined in
several units, but identically). The errors you will see:

| Message | Means |
| --- | --- |
| `'greet' was not declared in this scope` | the compiler never saw a declaration: a missing `#include` or a typo |
| `undefined reference to 'dojo::greet(...)'` | the linker found the declaration but no definition: a missing `.cpp` in the build, or a signature that differs |
| `multiple definition of 'helper'` | a non-inline function defined in a header included by two `.cpp` files |

**Modules** (C++20, and `import std;` in C++23) replace textual inclusion with compiled
interfaces and build faster, but build tools are still catching up; most code you will read
uses headers, and so does this track.

## Hello, world in C++23

```cpp
#include <print>

int main() {
    std::println("Hello, {}!", "world");   // C++23; before that std::cout << ... << '\n'
}
```

`main` returning without a `return` statement returns 0. `std::println` uses the same
format strings as `std::format`, checked at compile time.

## CMake

Real projects describe the build once and let **CMake** generate it for Ninja, Make or
Visual Studio. Every dojo kata ships this kind of `CMakeLists.txt`:

```cmake
cmake_minimum_required(VERSION 3.28)
project(kata LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)   # compile_commands.json for clangd and clang-tidy

add_executable(app main.cpp greet.cpp)
target_compile_options(app PRIVATE -Wall -Wextra -Wpedantic)
```

```bash
cmake -B build              # configure: generate the build files in build/
cmake --build build         # compile and link
ctest --test-dir build      # run the tests registered with CMake
```

Modern CMake is **target-based**: you attach sources, include directories, options and
libraries to targets (`target_link_libraries(app PRIVATE GTest::gtest_main)`), never to
global variables.

## The safety net: warnings, sanitizers and tools

Turn on warnings always (`-Wall -Wextra -Wpedantic`; `/W4` on MSVC) and treat them as bugs.
Then add the tools the dojo runs on every kata:

| Tool | Finds |
| --- | --- |
| **AddressSanitizer** (`-fsanitize=address`) | use after free, buffer overflows, leaks, at run time |
| **UndefinedBehaviorSanitizer** (`-fsanitize=undefined`) | signed overflow, bad shifts, null dereferences |
| **ThreadSanitizer** (`-fsanitize=thread`) | data races between threads |
| **clang-tidy** | bug patterns and Core Guidelines violations, statically |
| **clang-format** | formatting, so reviews talk about code instead of spaces |

Sanitizers slow the program down (2× for ASan, 5–15× for TSan) and belong in tests and CI,
not in release builds.

## How the dojo checks your C++

Each kata is a CMake project: headers and sources at the top, tests in `tests/`. When you
save, the dojo compiles your files with GCC 16 in a container, runs the GoogleTest tests
under the sanitizers, and adds one result each for warnings, clang-format and clang-tidy. A
function you still have to write throws `std::logic_error("todo: ...")`.

## Summary

- Preprocessor (text), compiler (one translation unit at a time), linker (joins them).
- Headers declare, one `.cpp` defines: the one definition rule.
- "Not declared" is the compiler; "undefined reference" is the linker.
- Choose the standard explicitly; this track uses C++23 on GCC 16.
- CMake describes targets; warnings, sanitizers, clang-tidy and clang-format catch what the
  compiler allows.

## Further reading

- cppreference, Translation phases: <https://en.cppreference.com/w/cpp/language/translation_phases>
- cppreference, Definitions and ODR: <https://en.cppreference.com/w/cpp/language/definition>
- cppreference, C++ compiler support: <https://en.cppreference.com/w/cpp/compiler_support>
- CMake tutorial (official): <https://cmake.org/cmake/help/latest/guide/tutorial/index.html>
- GCC, Options to request or suppress warnings: <https://gcc.gnu.org/onlinedocs/gcc/Warning-Options.html>
- Clang, AddressSanitizer: <https://clang.llvm.org/docs/AddressSanitizer.html>
- Bjarne Stroustrup, *A Tour of C++* (3rd edition), chapter 1
- roadmap.sh, C++ roadmap: <https://roadmap.sh/cpp>
