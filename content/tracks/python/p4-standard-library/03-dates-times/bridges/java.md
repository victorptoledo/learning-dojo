---
from: java
status: draft
concept: "Dates and times"
from_way: "java.time: Instant, LocalDateTime, ZonedDateTime with ZoneId, Duration and Period, plusDays that keeps the wall time, and an injected Clock."
native_way: "One datetime type, naive or aware, with ZoneInfo; timedelta for both durations and day steps; fold for ambiguous times; an injected now callable."
sensei_tip: "ZonedDateTime resolves gaps by moving forward and picks the earlier offset in a fold; Python does not resolve anything for you, so check fold and the round trip yourself."
---

| Java 25 | Python 3.15 |
| --- | --- |
| `Instant.now()` | `datetime.now(UTC)` |
| `LocalDateTime` | naive `datetime` |
| `ZonedDateTime.of(ldt, ZoneId.of("Europe/Lisbon"))` | `dt.replace(tzinfo=ZoneInfo("Europe/Lisbon"))` |
| `zdt.withZoneSameInstant(UTC)` | `dt.astimezone(UTC)` |
| `Duration.between(a, b)` | `b.astimezone(UTC) - a.astimezone(UTC)` |
| `date.get(IsoFields.WEEK_BASED_YEAR)` | `d.isocalendar().year` |
| `Clock.fixed(...)` | `now=lambda: fixed` |
