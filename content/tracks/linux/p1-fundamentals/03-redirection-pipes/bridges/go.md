---
from: go
status: draft
concept: "Streams of data between programs"
from_way: "fmt.Println to os.Stdout, log to os.Stderr, os.Exit, and loops with maps to count things."
native_way: "The same file descriptors 0, 1 and 2, and pipelines that connect programs."
sensei_tip: "bufio.Scanner over os.Stdin plus fmt.Println is all a Go filter needs. log writes to stderr by default, which keeps results and messages apart in a pipeline."
---

| Go | Linux |
| --- | --- |
| fmt.Println(x) | echo "$x" |
| fmt.Fprintln(os.Stderr, e) | echo "$e" >&2 |
| os.Exit(2) | exit 2 |
| counts[line]++ in a map | sort \| uniq -c |
| exec.Command(...).Output() | $(command) |
