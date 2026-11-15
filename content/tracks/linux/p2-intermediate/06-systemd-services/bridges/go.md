---
from: go
status: draft
concept: "Running an application as a service"
from_way: "A single binary started by hand or by a supervisor, often with its own daemon logic."
native_way: "systemd owns the lifecycle: the binary stays in the foreground, logs to stderr and handles SIGTERM."
sensei_tip: "Do not daemonise a Go program yourself: run it in the foreground and let systemd supervise it. github.com/coreos/go-systemd can send the ready notification for Type=notify."
---

| Go | Linux |
| --- | --- |
| ./server & disown | systemctl start server |
| custom restart loops | Restart=on-failure |
| log to a file | log to stderr, read with journalctl |
| a goroutine with time.Ticker for nightly jobs | a .timer unit |
