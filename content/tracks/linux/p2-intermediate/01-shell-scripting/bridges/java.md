---
from: java
status: draft
concept: "Writing small programs"
from_way: "Typed variables, methods, exceptions and a compiler that rejects mistakes before the program runs."
native_way: "Untyped strings, functions that return a status, exit codes instead of exceptions, and ShellCheck as the closest thing to a compiler."
sensei_tip: "When a script grows past a hundred lines, needs data structures or real error handling, write it in Java, Python or Go. Bash is glue between programs, not an application language."
---

| Java | Bash |
| --- | --- |
| `String name = "Ada";` | `name="Ada"` |
| `args[0]`, `args.length` | `"$1"`, `$#` |
| `if (s.isEmpty())` | `if [[ -z "$s" ]]` |
| `for (String h : hosts)` | `for h in "${hosts[@]}"` |
| `throw new IllegalArgumentException()` | `echo "error" >&2; exit 2` |
| `try { ... } finally { cleanup(); }` | `trap cleanup EXIT` |
| picocli or args4j | `getopts` |
