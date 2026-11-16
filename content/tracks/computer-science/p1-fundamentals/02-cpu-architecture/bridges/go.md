---
from: go
status: draft
concept: "What the CPU does with Go code"
from_way: "Treating goroutine stacks like OS thread stacks."
native_way: "The Go compiler emits machine code directly (with a register-based calling convention since Go 1.17); goroutine stacks start small and grow by copying, so deep recursion rarely overflows."
sensei_tip: "`go tool objdump` and `go test -bench . -cpuprofile` with `pprof -disasm` show which instructions are hot."
---

| In Go | Underneath |
| --- | --- |
| a function call | registers for arguments (ABIInternal) |
| goroutine stack | starts at a few KiB and grows |
| bounds checks | branches the compiler may eliminate |
| `//go:noinline` | keeps a call for experiments |
| `GOAMD64=v3` | lets the compiler use newer x86 instructions |
