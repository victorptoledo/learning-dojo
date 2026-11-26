---
from: java
status: draft
concept: "java.time"
from_way: "Instant, LocalDateTime, ZonedDateTime and ZoneId, Duration and Period, DateTimeFormatter patterns such as yyyy-MM-dd HH:mm, and Clock for tests."
native_way: "One time.Time type for an instant with a location, time.Duration for elapsed time, AddDate for calendar periods, layouts written as the reference time 2006-01-02 15:04, and an injected clock function or testing/synctest for tests."
sensei_tip: "Go has no LocalDateTime: a time.Time always has a location. Parse wall-clock text with ParseInLocation, and keep instants (UTC or with an offset) for anything you compare or store."
---

| Java | Go |
| --- | --- |
| `Instant.now()` | `time.Now()` |
| `ZonedDateTime` / `ZoneId.of("Europe/Lisbon")` | `t.In(loc)` / `time.LoadLocation("Europe/Lisbon")` |
| `LocalDateTime.parse(s, fmt).atZone(zone)` | `time.ParseInLocation(layout, s, loc)` |
| `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")` | `"2006-01-02 15:04"` |
| `Duration.ofSeconds(90)` | `90 * time.Second` |
| `plusDays(1)` / `Period` | `t.AddDate(0, 0, 1)` |
| `Duration.between(a, b)` | `b.Sub(a)` |
| `Clock.fixed(...)` in tests | a `now func() time.Time` field, or `testing/synctest` |
