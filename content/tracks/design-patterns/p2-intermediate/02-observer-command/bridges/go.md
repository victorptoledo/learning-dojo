---
from: go
status: draft
concept: "Observer, command and chain of responsibility"
from_way: "Calling every interested package directly from the function that changed the state."
native_way: "Channels for fan-out of events, slices of callback functions, commands as structs with a method or as `func()` values, and `http.Handler` middleware chains."
sensei_tip: "A Go channel is not a broadcast: each value goes to one receiver. For observer, keep a slice of subscriber channels (or callbacks) and send to each, without blocking on slow ones."
---

| Go | Pattern |
| --- | --- |
| a slice of `chan Event` per subscriber | observer |
| `context.Context` cancellation | unsubscribe |
| `func()` values in a queue | command |
| `func(next http.Handler) http.Handler` | chain of responsibility |
| a loop over `[]func(T) T` | pipeline |
