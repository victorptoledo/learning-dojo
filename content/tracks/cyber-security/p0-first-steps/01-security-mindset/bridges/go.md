---
from: go
status: draft
concept: "Thinking in threats, not only in features"
from_way: "A small net/http service where every handler is reachable and errors from checks are logged and ignored."
native_way: "A middleware that denies by default, handlers that return early on any failed check, and a list of what each endpoint exposes."
sensei_tip: "In Go, `if err != nil { return }` is the natural way to fail closed: make sure the error branch of an authorisation check writes 403 and stops instead of falling through."
---

| Go | Security mindset |
| --- | --- |
| log.Println(err) and continue | fail closed: 403 and return |
| net/http/pprof imported in production | a smaller attack surface |
| running the container as root | least privilege |
| one service account for everything | separate identities per service |
| secrets in command-line flags | secrets from files or a vault |
