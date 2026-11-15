---
from: go
status: draft
concept: "Process lifecycle and graceful shutdown"
from_way: "signal.NotifyContext(ctx, os.Interrupt, syscall.SIGTERM) cancels a context on a stop signal, and http.Server.Shutdown drains connections."
native_way: "That is exactly what docker stop needs: a Go binary as PID 1 with an exec-form ENTRYPOINT gets SIGTERM directly and shuts down cleanly."
sensei_tip: "Give Shutdown a timeout shorter than the stop grace period (10 s by default), or the drain is cut off by SIGKILL."
---

| Go | Container |
| --- | --- |
| `signal.NotifyContext` | receives the SIGTERM from `docker stop` |
| `server.Shutdown(ctx)` | graceful drain within the grace period |
| `os.Exit(1)` | exit code 1 |
| a systemd `Restart=on-failure` | `--restart on-failure` |
