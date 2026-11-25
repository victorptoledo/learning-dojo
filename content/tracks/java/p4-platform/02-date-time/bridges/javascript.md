---
from: javascript
status: draft
concept: "Dates and times with java.time"
from_way: "Date (a millisecond instant shown in local time) and libraries such as Luxon, or Temporal."
native_way: "java.time, which the Temporal proposal closely follows."
sensei_tip: "Temporal.Instant, PlainDate and ZonedDateTime map one to one to Instant, LocalDate and ZonedDateTime."
---

| JavaScript | Java 25 |
| --- | --- |
| `new Date().toISOString()` | `Instant.now().toString()` |
| `Temporal.PlainDate` | `LocalDate` |
| `Temporal.ZonedDateTime` | `ZonedDateTime` |
| `Temporal.Duration` | `Duration or Period` |
