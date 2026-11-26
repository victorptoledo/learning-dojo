---
from: go
status: draft
concept: "Dates and times"
from_way: "time.Time (always with a Location), time.LoadLocation, Duration, AddDate for calendar steps, a monotonic reading inside time.Now, and the reference layout 2006-01-02."
native_way: "datetime that may be naive, ZoneInfo, timedelta, a separate time.monotonic, and strftime codes like %Y-%m-%d."
sensei_tip: "Go's Sub uses the monotonic reading and absolute time; Python's subtraction of same-zone datetimes uses wall time. Convert to UTC before measuring."
---

| Go | Python 3.15 |
| --- | --- |
| `time.Now().UTC()` | `datetime.now(UTC)` |
| `time.LoadLocation("Europe/Lisbon")` | `ZoneInfo("Europe/Lisbon")` |
| `t.In(loc)` | `t.astimezone(loc)` |
| `b.Sub(a)` | `b.astimezone(UTC) - a.astimezone(UTC)` |
| `t.AddDate(0, 0, 1)` | `t + timedelta(days=1)` (wall time) |
| `t.Format("2006-01-02")` | `f"{t:%Y-%m-%d}"` |
| `t.ISOWeek()` | `t.isocalendar()[:2]` |
