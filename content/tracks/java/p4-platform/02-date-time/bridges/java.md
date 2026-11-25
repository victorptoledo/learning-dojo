---
from: java
status: draft
concept: "Dates and times with java.time"
from_way: "java.util.Date, Calendar and SimpleDateFormat: mutable, not thread-safe, months from 0."
native_way: "Immutable java.time types, one per idea, with zones and rules built in."
sensei_tip: "Never mix the two inside new code: convert at the boundary with date.toInstant() and Date.from(instant)."
---

| Old Java | Java 25 |
| --- | --- |
| `new Date()` | `Instant.now(clock)` |
| `Calendar.add(MONTH, 1)` | `date.plusMonths(1)` |
| `SimpleDateFormat (not thread-safe)` | `DateTimeFormatter (immutable)` |
| `TimeZone.getTimeZone("GMT+1")` | `ZoneId.of("Europe/Lisbon")` |
