---
from: csharp
status: draft
concept: "Streams of data between programs"
from_way: "Console.WriteLine and Console.Error, Environment.Exit, LINQ over collections, and Process.Start for other programs."
native_way: "stdout, stderr and exit codes shared by every program, and pipelines of small tools."
sensei_tip: "Return an int from Main (or set Environment.ExitCode) and write diagnostics to Console.Error; then your .NET tool composes with grep, sort and systemd like any Linux command."
---

| C# | Linux |
| --- | --- |
| Console.WriteLine(x) | echo "$x" |
| Console.Error.WriteLine(e) | echo "$e" >&2 |
| return 2; from Main | exit 2 |
| lines.GroupBy(l => l).OrderByDescending(g => g.Count()) | sort \| uniq -c \| sort -rn |
| Process.Start(...) | running a command |
