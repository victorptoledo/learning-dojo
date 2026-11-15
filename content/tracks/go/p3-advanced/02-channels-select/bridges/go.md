---
from: go
status: draft
concept: "Channel and timer habits that changed"
from_way: "time.After in hot loops leaking timers until they fired, draining t.C before Reset, channels used as iterators with a goroutine per traversal, and hand-made semaphores for every fan-out."
native_way: "Timers collected when unreferenced and timer channels unbuffered (1.23, always since 1.27), iter.Seq for iteration, wg.Go for fan-out, and errgroup.SetLimit for bounded groups."
sensei_tip: "Remove the `if !t.Stop() { <-t.C }` dance before Reset: with synchronous timer channels it is no longer needed, and since Go 1.27 the asynctimerchan setting is gone."
---

| Older Go | Go 1.27 |
| --- | --- |
| `if !t.Stop() { <-t.C }; t.Reset(d)` | `t.Reset(d)` |
| `time.After` in a loop leaking until it fires | timers are garbage collected when unreferenced |
| `GODEBUG=asynctimerchan=1` | removed in 1.27: timer channels are always synchronous |
| `func (t *Tree) Walk() <-chan Node` | `func (t *Tree) All() iter.Seq[Node]` |
| semaphore channel + WaitGroup | `errgroup.Group` with `SetLimit(n)` |
