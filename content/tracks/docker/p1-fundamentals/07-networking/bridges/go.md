---
from: go
status: draft
concept: "Service addresses and ports"
from_way: "http.ListenAndServe(\"localhost:8080\", h) is a common habit to avoid firewall prompts on a laptop."
native_way: "Use \":8080\" (all interfaces) in containers, and dial other services by name: net.Dial(\"tcp\", \"db:5432\") is resolved by Docker's DNS."
sensei_tip: "The pure-Go resolver reads /etc/resolv.conf, which Docker points at 127.0.0.11 on user-defined networks; nothing special is needed in a static binary."
---

| Go | Container networking |
| --- | --- |
| `ListenAndServe("localhost:8080", h)` | unreachable through `-p` |
| `ListenAndServe(":8080", h)` | correct in a container |
| `net.Dial("tcp", "db:5432")` | resolved by the embedded DNS |
| `net.Interfaces()` | `lo` and `eth0` of the container's namespace |
