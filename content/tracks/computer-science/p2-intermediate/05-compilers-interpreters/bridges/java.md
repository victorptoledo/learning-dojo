---
from: java
status: draft
concept: "Java's compilers"
from_way: "Thinking javac does the optimising."
native_way: "javac parses Java into an AST, checks it and emits stack-machine bytecode almost unoptimised; HotSpot's C1 and C2 JIT compilers optimise at run time with profiles; GraalVM native-image compiles ahead of time."
sensei_tip: "`javap -c Foo.class` shows the stack bytecode of your methods: compare it with what you emit in the checkpoint kata."
---

| In Java | Underneath |
| --- | --- |
| `javac` | lexer, parser, AST, attribution, bytecode |
| `javap -c` | the stack-machine instructions |
| `iload`, `iadd`, `istore` | LOAD, ADD, STORE |
| C1 / C2 | tiered JIT with profiling |
| ANTLR, JavaParser | parser generator, Java AST library |
