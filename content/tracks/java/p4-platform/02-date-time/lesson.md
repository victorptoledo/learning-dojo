# Dates and Times with java.time

Time bugs reach production quietly: a report one hour off twice a year, a deadline that moves
when a server changes region, a "daily" job that runs twice. The `java.time` API (Java 8+)
avoids most of them by giving each idea its own immutable type. The skill is picking the
right one.

## Which type for which idea

| Type | Means | Example |
| --- | --- | --- |
| `Instant` | a point on the timeline, in UTC | when a payment happened |
| `LocalDate` | a calendar date, no time, no zone | a birthday, an invoice due date |
| `LocalTime` | a wall-clock time, no date | "the shop opens at 09:00" |
| `LocalDateTime` | date and wall-clock time, **no zone** | what a person typed in a form |
| `ZonedDateTime` | date and time in a zone with its rules | a meeting at 09:00 in Lisbon |
| `OffsetDateTime` | date and time with a fixed offset | a timestamp in an API (`+01:00`) |
| `Duration` | an amount of machine time (seconds, nanos) | a timeout, hours worked |
| `Period` | an amount of calendar time (years, months, days) | "three months from today" |

A `LocalDateTime` is **not** a moment: "2026-03-29 01:30" happens at different instants in
Lisbon and in São Paulo. Store and compare **instants**; keep local types for what people
read and type.

## Zones and daylight saving time

A zone (`ZoneId.of("Europe/Lisbon")`) carries the rules: offsets and their changes. Use
region ids, not fixed offsets, whenever people live in that place.

```java
ZoneId lisbon = ZoneId.of("Europe/Lisbon");
ZonedDateTime start = LocalDateTime.parse("2026-03-29T00:30").atZone(lisbon);   // +00:00
ZonedDateTime end = LocalDateTime.parse("2026-03-29T03:30").atZone(lisbon);     // +01:00
Duration.between(start, end);                       // PT2H: the clocks skipped 01:00 to 02:00
```

On the night the clocks go forward, a wall-clock span of three hours lasts two; on the night
they go back, it lasts four. `Duration.between` on zoned times (or instants) counts real time;
on two `LocalDateTime` values it counts wall-clock time and is wrong across the change.

Two edge cases come from the same rules:

- **Gap:** 01:30 on that March night does not exist in Lisbon. `atZone` moves it forward
  (02:30, `+01:00`).
- **Overlap:** 01:30 on the October night happens twice. `atZone` picks the earlier offset;
  `withLaterOffsetAtOverlap()` picks the other.

## Calendar arithmetic

```java
LocalDate due = invoiceDate.plusMonths(1);       // 2026-01-31 + 1 month = 2026-02-28
long days = ChronoUnit.DAYS.between(start, end); // whole days between two dates
LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
int weekYear = date.get(IsoFields.WEEK_BASED_YEAR);   // 2026-12-31 is in week 53 of 2026; 2024-12-30 is week 1 of 2025
```

Use `Period` and `plusMonths` for calendar steps, `Duration` for clock steps. Adding
`Duration.ofDays(1)` to a `ZonedDateTime` adds 24 hours, which is not "the same time
tomorrow" on a DST night; `plusDays(1)` is.

For ISO weeks, pair `WEEK_OF_WEEK_BASED_YEAR` with `WEEK_BASED_YEAR`, never with
`getYear()`: the first days of January can belong to the last week of the previous year.

## Parsing and formatting

```java
LocalDate d = LocalDate.parse("2026-11-25");                        // ISO-8601 by default
var format = DateTimeFormatter.ofPattern("dd/MM/uuuu");
LocalDate typed = LocalDate.parse("25/11/2026", format);
String text = Instant.now().toString();                             // 2026-11-25T14:03:07.123Z
```

`DateTimeParseException` (unchecked) reports bad input. Prefer ISO-8601 in files and APIs;
it sorts as text and every language reads it. `uuuu` is the year; `yyyy` is the year of era
and surprises you with strict parsing.

## Testable "now": inject a Clock

`Instant.now()` and `LocalDate.now()` read the system clock, which a test cannot move. Take a
`Clock` (or a `Supplier<Instant>`) in the constructor:

```java
final class Billing {
    private final Clock clock;
    Billing(Clock clock) { this.clock = clock; }
    boolean overdue(Invoice invoice) {
        return LocalDate.now(clock).isAfter(invoice.due());
    }
}

// in a test
var clock = Clock.fixed(Instant.parse("2026-12-01T10:00:00Z"), ZoneOffset.UTC);
```

`LocalDate.now(clock)` also makes the zone explicit: "today" depends on where you are.

## Summary

- `Instant` for moments, local types for what people see, `ZonedDateTime` with a region id
  to convert between them.
- Count real time with `Duration.between` on instants or zoned times, never on local
  date-times across a DST change.
- Gaps move forward; overlaps take the earlier offset unless you ask.
- `Period`/`plusMonths` for calendar steps; ISO weeks use the week-based year.
- ISO-8601 in files and APIs; inject a `Clock` instead of calling `now()`.
