The building has rules, and they change without a new release: they live in
`application.properties`, which already has them.

```properties
booking.opens=08:00
booking.closes=20:00
booking.max-duration=PT4H
```

Read them into a validated `@ConfigurationProperties("booking")` record (all three required,
`opens` before `closes`) and **inject it** where it is needed. Never write the values in the
code: one contract test runs with other settings.

New rules for a booking, checked after the body and the room and **before** capacity and
overlaps, each one `422` with this detail:

| Rule | Detail |
| --- | --- |
| It does not start in the past | `a booking cannot start in the past` |
| It starts and ends on the same day, inside the opening hours | `the building is open from 08:00 to 20:00` |
| It lasts at most `max-duration` | `a booking lasts at most 240 minutes` |

"Now" comes from a `java.time.Clock` **bean** that you define (`Clock.systemDefaultZone()`)
and inject: never call `LocalDateTime.now()` without it. The contract tests put their own
clock in its place, fixed on a known day.

One new endpoint:

| Request | Answer |
| --- | --- |
| `GET /rooms/{roomId}/availability?date=2099-03-02` | `200`, the free slots of that day, by start |

A free slot is `{"start": "2099-03-02T08:00", "end": "2099-03-02T09:00"}`: every stretch of
the opening hours that no booking of that room covers. A day with no bookings is one slot
from opening to closing; a fully booked day is an empty list. The date is required (`400`
without it) and an unknown room is `404`.

> Tip: `@ConfigurationPropertiesScan` on the application class registers the record; `@Validated`
> and `@AssertTrue` on a method of the record check `opens` before `closes`. `Duration` and
> `LocalTime` properties convert from text on their own.
