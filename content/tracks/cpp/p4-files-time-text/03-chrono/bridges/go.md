---
from: go
status: draft
concept: "Time, dates and time zones"
from_way: "time.Duration, time.Time with a Location, time.LoadLocation, and the monotonic reading inside time.Now() used by time.Since."
native_way: "std::chrono durations with units in the type, separate clocks for measuring and for dates, calendar types and zoned_time."
sensei_tip: "Go hides the monotonic clock inside time.Time; in C++ you pick steady_clock or system_clock explicitly."
---

| Go | C++ |
| --- | --- |
| `5 * time.Second` | `5s` |
| `time.Since(start)` | `std::chrono::steady_clock::now() - start` |
| `time.Date(2026, 11, 26, 0, 0, 0, 0, time.UTC)` | `sys_days{2026y / November / 26}` |
| `time.LoadLocation("Europe/Lisbon")` | `std::chrono::locate_zone("Europe/Lisbon")` |
| `t.ISOWeek()` | `std::format("{:%G-W%V}", sys_days{d})` |
