---
from: csharp
status: draft
concept: "Time, dates and time zones"
from_way: "TimeSpan, DateTime and DateTimeOffset, DateOnly, TimeZoneInfo with IsInvalidTime and IsAmbiguousTime, and Stopwatch."
native_way: "std::chrono durations, sys_time, year_month_day, time_zone::get_info for gaps and overlaps, and steady_clock."
sensei_tip: "TimeZoneInfo.IsInvalidTime is local_info::nonexistent; IsAmbiguousTime is local_info::ambiguous."
---

| C# | C++ |
| --- | --- |
| `TimeSpan.FromMinutes(90)` | `90min` |
| `Stopwatch.StartNew()` | `std::chrono::steady_clock::now()` |
| `new DateOnly(2026, 11, 26)` | `2026y / November / 26` |
| `TimeZoneInfo.ConvertTimeToUtc(local, tz)` | `tz->to_sys(local, choose::earliest)` |
| `DateTime.DaysInMonth(y, m)` | `year_month_day_last{y / m / last}.day()` |
