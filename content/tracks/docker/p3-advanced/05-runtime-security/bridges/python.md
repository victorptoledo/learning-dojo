---
from: python
status: draft
concept: "Least privilege at run time"
from_way: "resource.setrlimit caps a process, and a service runs as an unprivileged user under systemd."
native_way: "The engine applies the limits from outside (--memory, --cpus, --pids-limit, --ulimit) and strips kernel privileges with --cap-drop ALL and no-new-privileges."
sensei_tip: "Python writes .pyc files next to modules on import; on a read-only root file system set PYTHONDONTWRITEBYTECODE=1 (or precompile in the image) to avoid warnings and failures."
---

| Python | Runtime security |
| --- | --- |
| `resource.setrlimit(RLIMIT_NOFILE, ...)` | `--ulimit nofile=...` |
| gunicorn `--workers` | sized from the CPU limit, not the host's cores |
| writing caches next to the code | `--read-only` plus a tmpfs |
| running as root to bind port 80 | listen on 8000 and drop every capability |
