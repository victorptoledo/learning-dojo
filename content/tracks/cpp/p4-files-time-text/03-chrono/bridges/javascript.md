---
from: javascript
status: draft
concept: "Time, dates and time zones"
from_way: "Date (milliseconds since the epoch, local or UTC getters), Intl.DateTimeFormat for zones, performance.now() for measuring, and Temporal in newer engines."
native_way: "Typed durations, sys_time for UTC, calendar types, zoned_time with the tz database, and steady_clock."
sensei_tip: "If you know Temporal, C++ chrono will feel familiar: Instant is sys_time, PlainDate is year_month_day, ZonedDateTime is zoned_time."
---

| JavaScript | C++ |
| --- | --- |
| `setTimeout(f, 5000)` | a `std::chrono::milliseconds` parameter: `5s` converts itself |
| `performance.now()` | `std::chrono::steady_clock::now()` |
| `new Date(Date.UTC(2026, 10, 26))` | `sys_days{2026y / November / 26}` (months start at 1) |
| `Intl.DateTimeFormat(..., { timeZone })` | `zoned_time{locate_zone(name), tp}` |
| `Temporal.Duration` | `std::chrono::duration` |
