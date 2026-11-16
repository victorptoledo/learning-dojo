---
from: java
status: draft
concept: "Compiling and building"
from_way: "javac compiles each class to bytecode, the JVM loads classes on demand, Maven or Gradle resolve dependencies and build jars."
native_way: "The preprocessor pastes headers, the compiler turns each .cpp into an object file, the linker joins them into a native executable; CMake describes the targets."
sensei_tip: "There is no classpath to find code at run time: every function you call must be declared in the translation unit and defined in exactly one object file."
---

| Java | C++ |
| --- | --- |
| `javac Foo.java` | `g++ -std=c++23 -c foo.cpp` (an object file) |
| `import com.x.Foo;` | `#include "foo.hpp"` (text pasted in) |
| a class file per class | a header for declarations, a .cpp for definitions |
| `ClassNotFoundException` at run time | `undefined reference` at link time |
| `pom.xml` / `build.gradle` | `CMakeLists.txt` (+ vcpkg or Conan for packages) |
| JIT, GC, one bytecode for every OS | native code per platform, no GC |
| `-Xlint`, SpotBugs | `-Wall -Wextra`, clang-tidy, sanitizers |
