---
from: javascript
status: draft
concept: "Shared state without a single thread"
from_way: "One thread runs JavaScript, so shared variables need no locks; SharedArrayBuffer with Atomics only when using workers; and race conditions only appear across await points."
native_way: "Goroutines run at the same time on several cores, so shared variables need a Mutex, an atomic or a channel, and the race detector checks it."
sensei_tip: "The hardest habit to unlearn from Node is that let count = 0; count++ is safe. In Go, two handlers doing that at once is a data race."
---

| JavaScript | Go |
| --- | --- |
| a module-level `let count` | `var count atomic.Int64` |
| `Atomics.add(view, 0, 1)` | `count.Add(1)` |
| a `Map` cache shared by handlers | `map` + `sync.Mutex` |
| a lazily created promise singleton | `sync.OnceValue` |
| async-mutex libraries | `sync.Mutex` |
| no data races in one thread | `go test -race` finds them |
