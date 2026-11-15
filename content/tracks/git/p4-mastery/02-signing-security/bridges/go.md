---
from: go
status: draft
concept: "Signing and secrets"
from_way: "Constants with credentials compiled into the binary for convenience."
native_way: "Configuration from the environment at run time; signed release tags; and govulncheck plus the checksum database for dependency integrity."
sensei_tip: "Strings compiled into a Go binary are readable with the strings command. A credential in a const is as public as the binary you ship."
---

| Go habit | Git practice |
| --- | --- |
| `const apiKey = "..."` | `os.Getenv("API_KEY")` |
| `go.sum` verifies module content | signatures verify who made a commit |
| signed release tags | `git tag -s v1.5.0` |
| leaked key in history | rotate, then `git filter-repo --replace-text` |
