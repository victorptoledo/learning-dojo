---
from: go
status: draft
concept: "The layers under a Go program"
from_way: "Assuming a compiled Go binary means there is no runtime underneath."
native_way: "The Go compiler produces machine code, but the binary includes a runtime: a scheduler that multiplexes goroutines on OS threads, a garbage collector and a network poller."
sensei_tip: "`go build -gcflags=-S` prints the assembly the compiler generated: Go makes the machine layer easy to look at."
---

| In Go | Underneath |
| --- | --- |
| `go build` | source straight to machine code |
| `go f()` | a goroutine, scheduled by the Go runtime on OS threads |
| `net.Dial` | a socket registered with the runtime's epoll poller |
| `float64` | IEEE 754 binary64 |
| `GOMAXPROCS` | how many OS threads run Go code at once |
