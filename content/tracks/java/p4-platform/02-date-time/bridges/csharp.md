---
from: csharp
status: draft
concept: "Dates and times with java.time"
from_way: "DateTime with Kind, DateTimeOffset, DateOnly and TimeZoneInfo."
native_way: "Instant, LocalDate, OffsetDateTime and ZonedDateTime with ZoneId."
sensei_tip: "DateTimeOffset is OffsetDateTime: it keeps an offset but not the rules; ZonedDateTime keeps the region and knows DST."
---

| C# | Java 25 |
| --- | --- |
| `DateTime.UtcNow` | `Instant.now()` |
| `DateOnly` | `LocalDate` |
| `DateTimeOffset` | `OffsetDateTime` |
| `TimeZoneInfo.ConvertTime` | `instant.atZone(zone)` |
