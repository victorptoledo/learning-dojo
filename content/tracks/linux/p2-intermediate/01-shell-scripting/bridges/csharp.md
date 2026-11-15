---
from: csharp
status: draft
concept: "Writing small programs"
from_way: "A compiled, typed language with exceptions; scripts in PowerShell or, since .NET 10, single-file C# run with dotnet run app.cs."
native_way: "Bash: untyped text, exit statuses and small programs glued together."
sensei_tip: "PowerShell exists on Linux too, but servers and container images do not have it by default; bash is always there. Keep deployment scripts in bash and check them with ShellCheck."
---

| C# | Linux |
| --- | --- |
| string name = "Ada"; | name="Ada" |
| args[0], args.Length | "$1", $# |
| string.IsNullOrEmpty(s) | [[ -z "$s" ]] |
| foreach (var h in hosts) | for h in "${hosts[@]}" |
| Environment.Exit(2) | exit 2 |
| try/finally | trap cleanup EXIT |
