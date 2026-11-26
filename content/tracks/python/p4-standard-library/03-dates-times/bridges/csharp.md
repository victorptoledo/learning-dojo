---
from: csharp
status: draft
concept: "Dates and times"
from_way: "DateTime with Kind, DateTimeOffset, TimeZoneInfo (IANA ids on .NET 6+), TimeSpan, Stopwatch and TimeProvider for tests."
native_way: "datetime naive or aware, ZoneInfo, timedelta, time.monotonic and an injected now callable."
sensei_tip: "DateTimeOffset stores an offset, not a zone; Python's aware datetime with ZoneInfo keeps the zone, so it knows the rules on other dates too."
---

| C# | Python 3.15 |
| --- | --- |
| `DateTimeOffset.UtcNow` | `datetime.now(UTC)` |
| `DateTime` with `Kind.Unspecified` | naive `datetime` |
| `TimeZoneInfo.FindSystemTimeZoneById("Europe/Lisbon")` | `ZoneInfo("Europe/Lisbon")` |
| `TimeZoneInfo.ConvertTime(d, tz)` | `d.astimezone(tz)` |
| `tz.IsAmbiguousTime(d)`, `IsInvalidTime(d)` | compare `fold=0` and `fold=1` offsets, round trip through UTC |
| `Stopwatch` | `time.perf_counter()` |
| `ISOWeek.GetYear(d)` | `d.isocalendar().year` |
