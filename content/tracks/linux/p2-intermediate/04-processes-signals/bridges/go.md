---
from: go
status: draft
concept: "Processes and stopping cleanly"
from_way: "goroutines inside one process, os/exec for others, and os/signal.Notify for signals."
native_way: "Processes, PIDs and signals at the operating system level."
sensei_tip: "signal.NotifyContext(ctx, syscall.SIGTERM, os.Interrupt) gives you a context that is cancelled on SIGTERM: pass it down and every goroutine stops at a safe point, like the flag in a bash trap."
---

| Go | Linux |
| --- | --- |
| exec.Command("ls").Start() | fork + exec |
| os.Getpid() | $$ |
| cmd.Wait() | wait |
| signal.Notify(ch, syscall.SIGTERM) | trap ... TERM |
| cmd.Process.Kill() | kill -9 PID |
