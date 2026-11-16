---
from: go
status: draft
concept: "os/exec in Node.js"
from_way: "exec.CommandContext(ctx, name, args...) with no shell, cmd.StdoutPipe, cmd.Wait returning an *ExitError, and context cancellation killing the process."
native_way: "spawn(name, args, { signal }) with no shell, child.stdout, the close event with code and signal, and abort() killing the process."
sensei_tip: "exec.CommandContext and spawn with a signal behave the same: cancel the context or abort the signal and the child is killed. Go's cmd.WaitDelay matches the SIGTERM-then-SIGKILL grace period you write in the kata."
---

| Go | Node.js |
| --- | --- |
| `exec.CommandContext(ctx, "git", "log")` | `spawn("git", ["log"], { signal })` |
| `cmd.Dir` / `cmd.Env` | `cwd` / `env` |
| `cmd.StdoutPipe()` | `child.stdout` |
| `err.(*exec.ExitError).ExitCode()` | `code` on close |
| `cmd.Cancel` + `cmd.WaitDelay` | `killSignal` + your own grace timer |
| `sh -c` | `exec` / `shell: true` |
