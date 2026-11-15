---
from: go
status: draft
concept: "Configuration outside the artifact"
from_way: "Flags and env vars read in main, with secrets passed as flags that show up in ps."
native_way: "ConfigMaps and Secrets become env vars or files; the binary reads them at start or watches the files."
sensei_tip: "Read secrets from files rather than flags (flags appear in the process list), and watch the mounted directory's ..data symlink with fsnotify to reload on rotation."
---

| Go world | Kubernetes |
| --- | --- |
| `flag.String("log-level", ...)` | `args` from a ConfigMap value |
| `os.Getenv("DB_PASSWORD")` | `secretKeyRef` |
| `os.ReadFile("/etc/app/config.yaml")` | a ConfigMap volume |
| fsnotify reload | the kubelet's atomic file updates |
