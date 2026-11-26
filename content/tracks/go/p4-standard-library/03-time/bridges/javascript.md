---
from: javascript
status: draft
concept: "Date, Temporal and time zones"
from_way: "Date objects counting milliseconds since 1970 in local time or UTC, Intl.DateTimeFormat for zones, date libraries such as date-fns or Luxon, and the newer Temporal API with ZonedDateTime and Duration."
native_way: "time.Time with nanoseconds and a location, LoadLocation and In for zones, time.Duration for elapsed time, AddDate for calendar steps, and layouts written as the reference time."
sensei_tip: "Date months count from 0; Go's time.Month counts from 1 (time.January is 1). And Go's Duration is nanoseconds, so time.Duration(30) is 30 ns, not 30 ms: always multiply by a unit."
---

| JavaScript | Go |
| --- | --- |
| `new Date()` | `time.Now()` |
| `Date.parse(iso)` | `time.Parse(time.RFC3339, s)` |
| `d.toISOString()` | `t.UTC().Format(time.RFC3339Nano)` |
| `Temporal.ZonedDateTime` / `timeZone` | `t.In(loc)` |
| `Temporal.Duration` / milliseconds | `time.Duration` (nanoseconds) |
| `setTimeout(fn, 1000)` | `time.AfterFunc(time.Second, fn)` |
| `performance.now()` differences | `time.Since(start)` (monotonic) |
| fake timers in Jest | `testing/synctest` |
