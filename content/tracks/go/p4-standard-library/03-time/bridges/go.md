---
from: go
status: draft
concept: "time since Go 1.20"
from_way: "Layouts written out by hand for dates alone, timers whose channels had to be drained before Reset, unstopped timers kept alive until they fired, and tests that slept for real or replaced time with a mock clock library."
native_way: "time.DateOnly, TimeOnly and DateTime (1.20), timers that are collected when unreferenced and have unbuffered channels (1.23), and testing/synctest with a fake clock (stable in 1.25)."
sensei_tip: "Since Go 1.23, Stop and Reset on a timer never leave a stale value in its channel, so the old drain-before-Reset dance is gone. Many mock clock libraries can give way to synctest."
---

| Older Go | Go 1.27 |
| --- | --- |
| `"2006-01-02"` written by hand | `time.DateOnly` |
| `"2006-01-02 15:04:05"` | `time.DateTime` |
| `if !t.Stop() { <-t.C }` before `Reset` | `t.Reset(d)` (1.23) |
| `defer timer.Stop()` to avoid a leak | unreferenced timers are collected (1.23) |
| `time.Sleep` in tests | `synctest.Test` with a fake clock |
| a third-party mock clock | `testing/synctest` |
