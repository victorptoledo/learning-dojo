---
from: go
status: draft
concept: "Declare the state, let a tool converge to it"
from_way: "A shell script installs the binary, writes the unit file and restarts the service on every deploy."
native_way: "The state (binary version, unit file, running service) is declared and converged; or the binary is baked into an image and servers are replaced."
sensei_tip: "Go's single static binary makes immutable deployment easy: a new image per release, no packages to manage on the host."
---

| Go world | Configuration management |
| --- | --- |
| `install.sh` | an idempotent playbook |
| `os.WriteFile` every time | compare, then write |
| a reconcile loop in a controller | converging to desired state |
| a new binary per release | an immutable image |
