---
from: go
status: draft
concept: "Goroutine habits that changed"
from_way: "wg.Add(1) before go and defer wg.Done() inside, url := url before every closure, go.uber.org/automaxprocs in every container image, and leaks found by diffing goroutine dumps by hand."
native_way: "wg.Go (1.25), per-iteration loop variables (1.22), container-aware GOMAXPROCS (1.25), and the goroutineleak profile (1.27)."
sensei_tip: "Remove the automaxprocs import from services built with Go 1.25 or later; the runtime does it better, including when the CPU limit changes at run time."
---

| Older Go | Go 1.27 |
| --- | --- |
| `wg.Add(1); go func() { defer wg.Done(); ... }()` | `wg.Go(func() { ... })` |
| `for _, u := range urls { u := u; go f(u) }` | `for _, u := range urls { go f(u) }` |
| `import _ "go.uber.org/automaxprocs"` | nothing: GOMAXPROCS follows cgroup limits |
| diffing `/debug/pprof/goroutine?debug=2` dumps | `/debug/pprof/goroutineleak` |
| tracebacks without context | goroutine labels in tracebacks (1.27) |
