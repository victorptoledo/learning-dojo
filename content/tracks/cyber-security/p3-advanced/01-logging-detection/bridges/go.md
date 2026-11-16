---
from: go
status: draft
concept: "Logs that a detection can trust"
from_way: "log.Printf(\"login %s\", user) to stderr."
native_way: "log/slog with a JSON handler, typed attributes, a ReplaceAttr hook for redaction, and a request id from context."
sensei_tip: "`slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{ReplaceAttr: redact}))` gives structured logs from the standard library; a type implementing `slog.LogValuer` can hide its own secret fields."
---

| Go | Security logging |
| --- | --- |
| log.Printf("%s", user) | slog.Info("login_failed", "user", id) |
| printing whole structs | LogValuer that omits secrets |
| text logs on disk | JSON to stdout, shipped centrally |
| no request id | an id stored in context |
| panics without context | recovered and logged with attributes |
