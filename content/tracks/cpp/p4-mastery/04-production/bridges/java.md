---
from: java
status: draft
concept: "C++ in production"
from_way: "Maven Central, a stable bytecode ABI, the JVM's memory safety, JNI or the Foreign Function and Memory API for native code."
native_way: "vcpkg or Conan, a fragile binary ABI, memory safety through tooling and hardening, and C interfaces for interop."
sensei_tip: "The JVM guaranteed memory safety for you; in C++ it is a process: sanitizers in CI, fuzzing, hardening flags and code review."
---

| Java | C++ |
| --- | --- |
| Maven Central | vcpkg, Conan |
| stable bytecode across JVMs | ABI tied to compiler and standard library |
| memory safety by design | sanitizers, fuzzing, hardening, safe style |
| JNI / FFM API | `extern "C"` functions |
| jar versions | shared library sonames, pimpl |
| JFR in production | crash dumps, perf, logs and metrics |
