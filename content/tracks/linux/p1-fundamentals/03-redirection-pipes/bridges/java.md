---
from: java
status: draft
concept: "Streams of data between programs"
from_way: "System.out and System.err inside one program, and java.util.stream pipelines over collections in memory."
native_way: "stdout and stderr between programs, and pipelines of separate processes that stream text to each other while they run."
sensei_tip: "A Java service in a container should log to stdout and stderr, not to files: the container runtime and systemd collect those streams for you."
---

| Java | Shell |
| --- | --- |
| `System.out.println(x)` | `echo "$x"` (stdout, fd 1) |
| `System.err.println(e)` | `echo "$e" >&2` (stderr, fd 2) |
| `System.exit(2)` | `exit 2` |
| `lines.stream().filter(...).count()` | `grep ... \| wc -l` |
| `Collectors.groupingBy(f, counting())` | `sort \| uniq -c` |
| `.sorted(comparing(...).reversed()).limit(3)` | `sort -rn \| head -n 3` |
