# Setting Up JDK 25

Java changed a lot after Java 8. Since 2018 a new version ships every six months, and every
two years one of them is a **long-term support** (LTS) release: 11, 17, 21 and now **25**.
This track teaches Java 25. Everything you knew from Java 8 still compiles, but the language
around it grew: records, sealed types, pattern matching, `var`, text blocks, virtual threads.

In this lesson you install the JDK, run Java three ways, and see how the dojo checks your
Java katas.

## What you install

| Piece | What it is |
| --- | --- |
| **JDK** (Java Development Kit) | The compiler `javac`, the launcher `java`, `jshell` and the standard library. |
| **JRE** | Only the runtime. Since Java 11 there is no separate JRE download: install the JDK. |
| **Distribution** | Who builds the JDK: Eclipse Temurin, Oracle, Amazon Corretto, Azul Zulu... They are built from the same OpenJDK sources. The dojo uses Temurin. |
| **Maven** | The build tool: it downloads libraries and runs the tests. Optional for the katas. |

> [!NOTE]
> You do **not** need a JDK on your machine to complete the katas. The dojo checks them in a
> Docker container that already has JDK 25 and JUnit 5. A local JDK lets VS Code show errors
> as you type and run or debug the visible tests.

### Install it on Windows

```powershell
winget install EclipseAdoptium.Temurin.25.JDK
```

Close and reopen the terminal, then check:

```powershell
java --version
javac --version
```

Both should print `25.x`. If `java` is still an older version, an older JDK comes first in
your `PATH`: remove it, or move the Temurin 25 `bin` folder to the top.

In VS Code, install the **Extension Pack for Java** (`vscjava.vscode-java-pack`). It reads
`pom.xml`, shows errors as you type and adds **Run Test** and **Debug Test** above each test.

## Three ways to run Java

### 1. jshell: try one line at a time

`jshell` is a REPL. Type an expression, see the result. Perfect for checking how something
behaves before you put it in code.

```text
$ jshell
jshell> "  Ferris ".strip()
$1 ==> "Ferris"

jshell> List.of(3, 1, 2).stream().sorted().toList()
$2 ==> [1, 2, 3]

jshell> /exit
```

### 2. A source file, no compile step

Since Java 11 the launcher can run a `.java` file directly: it compiles in memory and runs.
Since **Java 25** a file does not even need a class around `main`. Save this as `Hello.java`:

```java
void main() {
    String name = IO.readln("Your name: ");
    IO.println("Welcome to the dojo, " + name + "!");
}
```

```powershell
java Hello.java
```

This is a **compact source file**: the compiler wraps the methods in an implicit class, and
`main` no longer needs `public static` or `String[] args`. `IO.println` and `IO.readln` are
small helpers in `java.lang`, so they need no import. It is made for scripts and first
steps; real projects still use classes and packages.

### 3. javac, then java

The classic way, and what build tools do for you:

```powershell
javac -d out src/main/java/dojo/Greeter.java
java -cp out dojo.Greeter
```

`javac` turns `.java` source into `.class` bytecode in `out/`; `java` starts the JVM and runs
the bytecode. The JVM compiles the hot parts to machine code while the program runs (the JIT).

## How a Java kata is laid out

Every Java kata is a small **Maven project**. You never have to run Maven, but the layout is
the standard one, so any IDE understands it:

```text
hello-dojo/
├── README.md                 the challenge
├── pom.xml                   project description: Java 25, JUnit 5
└── src/
    ├── main/java/dojo/
    │   └── Greeter.java      your code
    └── test/java/dojo/
        └── GreeterTest.java  visible tests you can read and run
```

- `src/main/java` holds the code, `src/test/java` the tests. The folders under them follow
  the **package**: `package dojo;` lives in `dojo/`.
- The methods you must write start with
  `throw new UnsupportedOperationException("todo ...")`. That line is a placeholder: the
  tests fail until you replace it.
- When you save a file, the dojo copies your project into a container, adds **hidden grading
  tests**, compiles everything with `javac` and runs JUnit. The result appears in the kata
  panel.

## A first look at a JUnit 5 test

```java
package dojo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GreeterTest {
    @Test
    void greets_by_name() {
        assertEquals("Welcome to the dojo, Ada!", Greeter.greet("Ada"));
    }
}
```

- `@Test` marks a test method. It needs no `public` in JUnit 5.
- `assertEquals(expected, actual)`: expected first. When it fails you read
  `expected: <...> but was: <...>`.
- The dojo shows the test name with spaces, so names like `greets_by_name` read as sentences.

## Strings you will use right away

| Method | Result |
| --- | --- |
| `"  Ada ".strip()` | `"Ada"` (removes Unicode whitespace at both ends) |
| `"  Ada ".trim()` | `"Ada"` (older: removes characters ≤ space) |
| `"".isEmpty()` | `true` |
| `"   ".isBlank()` | `true` (empty or only whitespace) |
| `"Hi %s!".formatted("Ada")` | `"Hi Ada!"` (same as `String.format("Hi %s!", "Ada")`) |

> [!TIP]
> Prefer `strip()` and `isBlank()` (Java 11) over `trim()` and `trim().isEmpty()`. They
> understand Unicode spaces, not only ASCII ones.

## Summary

- Install **Temurin 25**; `java --version` must say 25.
- `jshell` for experiments, `java File.java` for scripts, `javac` + `java` (or Maven) for
  projects.
- A Java kata is a Maven project: your code in `src/main/java`, visible tests in
  `src/test/java`, hidden grading tests added by the dojo.
- Replace every `throw new UnsupportedOperationException("todo ...")` with real code.

Next: your first Java kata. Press **Q** for this lesson's quiz first.
