---
from: go
status: draft
concept: "Dates and times with java.time"
from_way: "time.Time with a Location, time.Duration and the reference layout 2006-01-02."
native_way: "Separate types for instants, local dates and zoned times, and pattern letters for formats."
sensei_tip: "A Go time.Time is always an instant plus a location; Java adds types with no zone at all for dates people read and type."
---

| Go | Java 25 |
| --- | --- |
| `time.Now().UTC()` | `Instant.now()` |
| `time.LoadLocation("Europe/Lisbon")` | `ZoneId.of("Europe/Lisbon")` |
| `t.Sub(u)` | `Duration.between(u, t)` |
| `t.Format("2006-01-02")` | `date.toString() or DateTimeFormatter` |
