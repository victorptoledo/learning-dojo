---
from: csharp
status: draft
concept: "DateTime, DateTimeOffset and TimeZoneInfo"
from_way: "DateTime with a Kind, DateTimeOffset for instants, TimeZoneInfo.ConvertTime, TimeSpan, custom format strings such as yyyy-MM-dd HH:mm, Stopwatch for measuring, and TimeProvider for tests."
native_way: "time.Time for instants with a location, t.In(loc) for conversion, time.Duration, layouts written as the reference time, time.Since on the monotonic clock, and an injected clock or synctest for tests."
sensei_tip: "DateTimeOffset is the closest match to time.Time; DateTime with Kind.Unspecified has no Go equivalent, and you should not try to build one. TimeZoneInfo.IsInvalidTime has no Go function: compare t.Format(layout) with the input."
---

| C# | Go |
| --- | --- |
| `DateTimeOffset.UtcNow` | `time.Now().UTC()` |
| `TimeZoneInfo.FindSystemTimeZoneById(id)` | `time.LoadLocation(id)` |
| `TimeZoneInfo.ConvertTime(t, zone)` | `t.In(loc)` |
| `DateTime.ParseExact(s, "yyyy-MM-dd HH:mm", ...)` | `time.ParseInLocation("2006-01-02 15:04", s, loc)` |
| `TimeSpan.FromMinutes(90)` | `90 * time.Minute` |
| `t.AddDays(1)` | `t.AddDate(0, 0, 1)` |
| `Stopwatch.StartNew()` | `start := time.Now()`; `time.Since(start)` |
| `TimeProvider` / `FakeTimeProvider` | an injected clock, or `testing/synctest` |
