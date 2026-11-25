---
from: python
status: draft
concept: "Dates and times with java.time"
from_way: "datetime with tzinfo (aware or naive), zoneinfo and timedelta."
native_way: "Separate types: Instant, LocalDateTime (naive), ZonedDateTime (aware), Duration and Period."
sensei_tip: "A naive datetime is a LocalDateTime; an aware one is a ZonedDateTime. Java makes the difference a compile-time type."
---

| Python | Java 25 |
| --- | --- |
| `datetime.now(timezone.utc)` | `Instant.now()` |
| `datetime(2026, 3, 29, 1, 30)` | `LocalDateTime.of(2026, 3, 29, 1, 30)` |
| `dt.replace(tzinfo=ZoneInfo("Europe/Lisbon"))` | `ldt.atZone(ZoneId.of("Europe/Lisbon"))` |
| `timedelta(hours=2)` | `Duration.ofHours(2)` |
