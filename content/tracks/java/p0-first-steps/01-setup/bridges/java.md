---
from: java
status: draft
concept: "From Java 8 to Java 25"
from_way: "An Oracle JRE plus a separate JDK, `public static void main(String[] args)` in a class, and `javac` before every run."
native_way: "One JDK from any OpenJDK build (Temurin here), `java Hello.java` to run a file directly, and `void main()` in a compact source file."
sensei_tip: "Keep `jshell` open while you study this track: most questions about a method are answered in one line there."
---

| Java 8 habit | Java 25 |
| --- | --- |
| Download the JRE to run, the JDK to build | Only the JDK; no separate JRE since Java 11 |
| `javac Hello.java && java Hello` | `java Hello.java` |
| `public static void main(String[] args)` | `void main()` in a compact source file |
| `System.out.println(...)` | still fine; `IO.println(...)` in small programs |
| `"  x ".trim().isEmpty()` | `"  x ".isBlank()` |
| Releases every few years | A release every six months, LTS every two years |
