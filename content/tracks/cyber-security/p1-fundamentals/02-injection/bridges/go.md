---
from: go
status: draft
concept: "Keep code and data apart"
from_way: "fmt.Sprintf to build SQL and exec.Command(\"sh\", \"-c\", cmd)."
native_way: "db.Query with placeholders ($1 or ?), exec.Command with separate arguments, and os.Root or filepath checks for paths."
sensei_tip: "Go 1.24 added `os.Root`, which opens files only beneath a directory and refuses `..` and symlinks that escape it: the simplest traversal defence in Go."
---

| Go | Injection-safe way |
| --- | --- |
| db.Query(fmt.Sprintf(...)) | db.Query("... $1", name) |
| exec.Command("sh", "-c", cmd) | exec.Command("ping", "-c", "1", host) |
| filepath.Join(base, name) | os.OpenRoot(base) then root.Open(name) |
| dynamic ORDER BY | a map of allowed columns |
| sqlx.In with care | one placeholder per value |
