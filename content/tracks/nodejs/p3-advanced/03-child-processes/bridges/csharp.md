---
from: csharp
status: draft
concept: "System.Diagnostics.Process"
from_way: "ProcessStartInfo with ArgumentList, RedirectStandardOutput, UseShellExecute = false, WaitForExitAsync with a CancellationToken, and Kill(entireProcessTree)."
native_way: "spawn with an argument array, stdio \"pipe\" streams, no shell by default, the close event, a signal option to cancel and kill to stop."
sensei_tip: "ArgumentList (not the Arguments string) is the safe API in .NET, and the array of spawn is its twin. Node.js has no Kill(entireProcessTree): avoid shells so there is no hidden grandchild."
---

| C# | Node.js |
| --- | --- |
| `psi.ArgumentList.Add(x)` | `spawn(cmd, [x])` |
| `RedirectStandardOutput = true` | `stdio: "pipe"` (default) |
| `UseShellExecute = true` | `shell: true` (avoid) |
| `await p.WaitForExitAsync(token)` | `await once(child, "close")` + `{ signal }` |
| `p.ExitCode` | `code` of the close event |
| `p.Kill()` | `child.kill("SIGKILL")` |
