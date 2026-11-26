---
from: python
status: draft
concept: "Time, dates and time zones"
from_way: "datetime, timedelta, zoneinfo.ZoneInfo, and time.monotonic for measuring; fold=0/1 picks a side of an overlap."
native_way: "std::chrono durations and time points, calendar types, time_zone with choose::earliest and latest, and steady_clock."
sensei_tip: "Python's fold=0 is choose::earliest and fold=1 is choose::latest; C++ throws instead of guessing when you choose nothing."
---

| Python | C++ |
| --- | --- |
| `timedelta(minutes=90)` | `90min` |
| `time.monotonic()` | `std::chrono::steady_clock::now()` |
| `date(2026, 2, 29)` raises | `(2026y / February / 29).ok()` is false |
| `ZoneInfo("Europe/Lisbon")` | `std::chrono::locate_zone("Europe/Lisbon")` |
| `d.isocalendar().week` | `std::format("{:%V}", sys_days{d})` |
