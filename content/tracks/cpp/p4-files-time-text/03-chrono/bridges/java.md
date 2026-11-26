---
from: java
status: draft
concept: "Time, dates and time zones"
from_way: "java.time: Duration, Instant, LocalDate, ZonedDateTime with ZoneId, and System.nanoTime for measuring."
native_way: "std::chrono durations, sys_time, year_month_day, zoned_time with locate_zone, and steady_clock for measuring."
sensei_tip: "java.time and C++20 chrono share their design ideas: separate types for instants, local dates and zoned times."
---

| Java | C++ |
| --- | --- |
| `Duration.ofSeconds(5)` | `std::chrono::seconds{5}` or `5s` |
| `System.nanoTime()` | `std::chrono::steady_clock::now()` |
| `LocalDate.of(2026, 11, 26)` | `2026y / November / 26` |
| `ZonedDateTime.of(ldt, ZoneId.of("Europe/Lisbon"))` | `zone->to_sys(local, choose::earliest)` |
| `YearMonth.atEndOfMonth()` | `y / m / std::chrono::last` |
