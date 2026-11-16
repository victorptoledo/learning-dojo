---
from: csharp
status: draft
concept: "Console applications"
from_way: "string[] args or System.CommandLine, Environment.GetEnvironmentVariable, Console.Out and Console.Error, Environment.ExitCode, and Console.CancelKeyPress."
native_way: "process.argv with util.parseArgs, process.env, process.stdout and stderr, process.exitCode, and process.on(\"SIGINT\")."
sensei_tip: "process.exitCode is Environment.ExitCode: set it and return. Configuration from environment variables is the same twelve-factor idea as ASP.NET Core's environment provider, without the binding: validate it yourself."
---

| C# | Node.js |
| --- | --- |
| System.CommandLine | `util.parseArgs` |
| `Environment.GetEnvironmentVariable` | `process.env` |
| `Console.Error.WriteLine` | `console.error` |
| `Environment.ExitCode = 1` | `process.exitCode = 1` |
| `Console.CancelKeyPress` | `process.on("SIGINT")` |
| `dotnet tool install -g` | `npm install -g` / `npx` |
