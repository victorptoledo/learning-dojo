---
from: python
status: draft
concept: "datetime, zoneinfo and strftime"
from_way: "datetime objects that are naive or aware, zoneinfo.ZoneInfo for zones, timedelta, strftime and strptime with %Y-%m-%d, and time.monotonic for measuring."
native_way: "time.Time is always aware (it has a location), time.Duration is an int64 of nanoseconds, layouts are written by example, and time.Now carries a monotonic reading that Sub uses on its own."
sensei_tip: "There are no naive times in Go, and that is a gift: the bug of comparing a naive and an aware datetime does not exist. The new trap is the layout: 2006-01-02, not %Y-%m-%d."
---

| Python | Go |
| --- | --- |
| `datetime.now(timezone.utc)` | `time.Now().UTC()` |
| `ZoneInfo("Europe/Lisbon")` | `time.LoadLocation("Europe/Lisbon")` |
| `datetime.strptime(s, "%Y-%m-%d %H:%M")` | `time.Parse("2006-01-02 15:04", s)` |
| `dt.isoformat()` | `t.Format(time.RFC3339)` |
| `timedelta(minutes=90)` | `90 * time.Minute` |
| `time.monotonic()` | `start := time.Now()`; `time.Since(start)` |
| `dt.date()` | `t.Format(time.DateOnly)` or `t.Date()` |
| freezegun in tests | an injected clock, or `testing/synctest` |
