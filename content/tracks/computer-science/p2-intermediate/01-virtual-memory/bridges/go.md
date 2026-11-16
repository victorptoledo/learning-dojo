---
from: go
status: draft
concept: "Virtual memory and Go"
from_way: "Seeing high VSZ for a Go process and assuming a leak."
native_way: "The Go runtime reserves large address ranges and returns unused pages to the OS lazily; GOMEMLIMIT sets a soft limit the GC respects; syscall.Mmap maps files."
sensei_tip: "Set GOMEMLIMIT a little below the container limit; read `runtime/metrics` or `/debug/pprof/heap` before guessing."
---

| In Go | Underneath |
| --- | --- |
| VSZ in the gigabytes | reserved, untouched address space |
| `GOMEMLIMIT` | a soft memory limit for the GC |
| `debug.FreeOSMemory()` | return pages to the OS now |
| `syscall.Mmap` | map a file |
| `fatal error: out of memory` | the OS refused the runtime |
