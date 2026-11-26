---
from: javascript
status: draft
concept: "Dates and times"
from_way: "Date (always an instant, shown in the host's zone), Intl.DateTimeFormat with timeZone for display, Temporal where available, and libraries such as Luxon or date-fns-tz."
native_way: "datetime with ZoneInfo in the standard library: aware values that do arithmetic and conversion in any IANA zone."
sensei_tip: "A JavaScript Date is always UTC inside; a Python datetime may be naive. Make naive values an error at the boundary of your code."
---

| JavaScript | Python 3.15 |
| --- | --- |
| `new Date()` | `datetime.now(UTC)` |
| `Date.parse("2026-11-26T09:30:00Z")` | `datetime.fromisoformat("2026-11-26T09:30:00Z")` |
| `d.toISOString()` | `d.astimezone(UTC).isoformat()` |
| `Temporal.ZonedDateTime` | aware `datetime` with `ZoneInfo` |
| `Temporal.Duration` | `timedelta` |
| `performance.now()` | `time.perf_counter()` |
| `vi.useFakeTimers()` | an injected `now` |
