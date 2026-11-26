# Dates and Times with datetime and zoneinfo

Time bugs are the ones that pass every test in November and break in March. Python's
`datetime` module has everything you need, plus a few traps that only show up around daylight
saving time. The rules: keep instants in UTC, attach a real time zone with `zoneinfo` when a
person's local time matters, and measure durations with a monotonic clock.

## Naive and aware

A `datetime` without `tzinfo` is **naive**: "10:00", but nobody knows where. With `tzinfo` it
is **aware** and names one instant.

```python
from datetime import UTC, date, datetime, time, timedelta
from zoneinfo import ZoneInfo

now = datetime.now(UTC)                       # aware, the current instant
naive = datetime(2026, 3, 29, 1, 30)          # naive: a wall-clock reading, no zone
lisbon = ZoneInfo("Europe/Lisbon")
meeting = datetime(2026, 3, 29, 9, 0, tzinfo=lisbon)   # 09:00 in Lisbon, aware
```

- `datetime.utcnow()` and `utcfromtimestamp()` are deprecated: they return **naive** values
  that look like UTC but are not marked as UTC. Use `datetime.now(UTC)`.
- Naive and aware values cannot be compared or subtracted (`TypeError`). That is a feature.
- `ZoneInfo` reads the IANA database from the system, or from the `tzdata` package on
  Windows (`uv add tzdata`). Zone names are `Region/City`; never use fixed offsets such as
  `-03:00` for a place, because places change their rules.

**Converting** keeps the instant and changes the wall clock: `meeting.astimezone(UTC)` is
08:00 UTC. To attach a zone to a naive reading, use `naive.replace(tzinfo=lisbon)`.

## Daylight saving time: gaps and folds

Twice a year the local clock jumps:

- **Gap (spring forward).** In Lisbon, on 2026-03-29, the clock goes from 00:59:59 to 02:00.
  `01:30` never happens there.
- **Fold (fall back).** On 2026-10-25 the clock goes from 01:59:59 back to 01:00, so `01:30`
  happens **twice**.

Python does not raise for either. It uses the `fold` attribute (PEP 495) to choose: `fold=0`
is the first occurrence, `fold=1` the second. For a time inside a gap, the result is a time
that does not exist, and its offset follows `fold`.

```python
first = datetime(2026, 10, 25, 1, 30, tzinfo=lisbon)            # WEST, UTC+1
second = datetime(2026, 10, 25, 1, 30, fold=1, tzinfo=lisbon)   # WET,  UTC+0
second.astimezone(UTC) - first.astimezone(UTC)                  # timedelta(hours=1)
```

A reliable test for "does this local time exist, and only once?" is the round trip through
UTC:

```python
def resolve(local: datetime) -> datetime:
    """Return the aware time, or raise if it falls in a gap or is ambiguous."""
    a = local.replace(fold=0)
    b = local.replace(fold=1)
    if a.utcoffset() == b.utcoffset():
        return a                                     # normal time: one meaning
    if a.astimezone(UTC).astimezone(local.tzinfo).replace(tzinfo=None) != a.replace(tzinfo=None):
        raise ValueError(f"{local:%Y-%m-%d %H:%M} does not exist (DST gap)")
    raise ValueError(f"{local:%Y-%m-%d %H:%M} is ambiguous (DST fold)")
```

In a gap, converting to UTC and back gives a **different** wall time (01:30 becomes 02:30);
in a fold, both offsets round-trip.

## Arithmetic: wall time versus elapsed time

This surprises people coming from other languages: **adding a `timedelta` to an aware
datetime adds wall-clock time in its own zone**, and subtracting two datetimes **with the same
tzinfo** ignores the offsets.

```python
start = datetime(2026, 3, 28, 12, 0, tzinfo=lisbon)
start + timedelta(days=1)      # 2026-03-29 12:00 Lisbon: "same time tomorrow"
end = datetime(2026, 3, 29, 12, 0, tzinfo=lisbon)
end - start                    # timedelta(days=1): but only 23 hours passed
end.astimezone(UTC) - start.astimezone(UTC)   # timedelta(seconds=82800): 23 hours
```

So: "same time tomorrow" is wall-time arithmetic in the local zone; "how long did it take" and
"30 minutes from now" are arithmetic **in UTC**. Store and compute in UTC, convert to local
only to display or to apply a human rule (opening hours, "every day at 9").

## Durations: a monotonic clock

`datetime.now()` and `time.time()` follow the system clock, which NTP can move backwards.
For "how long did this take", use `time.monotonic()` (or `time.perf_counter()` for
benchmarks). Neither tells you the date; both only go forward.

## Parsing and formatting

```python
datetime.fromisoformat("2026-11-26T09:30:00+00:00")     # ISO 8601, also "Z" since 3.11
date.fromisoformat("2026-11-26")
datetime.strptime("26/11/2026 09:30", "%d/%m/%Y %H:%M")  # naive: attach the zone afterwards
stamp.isoformat()                                        # '2026-11-26T09:30:00+00:00'
f"{stamp:%Y-%m-%d %H:%M}"
```

- Exchange times as ISO 8601 with an offset (or in UTC with `Z`), never as local text.
- `strptime` gives a naive value; `%z` parses an offset, but an offset is not a zone.
- `%A`, `%B` and `%p` depend on the locale; keep them for display.

## Calendar arithmetic

- **ISO weeks.** `d.isocalendar()` returns `(year, week, weekday)`. The ISO year can differ
  from the calendar year: 2026-12-31 is in week 53 of 2026, and 2027-01-01 too, while
  2029-12-31 is in week 1 of **2030**. Group by `(iso_year, week)`, never by `(d.year, week)`.
- **Months** have no fixed length, so `timedelta` has no `months`. For "the first of next month"
  build the date: `date(y + m // 12, m % 12 + 1, 1)` with `y, m = d.year, d.month`.
- `date.weekday()` is 0 for Monday; `isoweekday()` is 1 for Monday.

## Testing time

Code that calls `datetime.now()` inside is hard to test. Inject the clock:

```python
def overdue(invoices, *, now: Callable[[], datetime] = lambda: datetime.now(UTC)): ...

def test_overdue_on_the_due_date():
    fixed = datetime(2026, 3, 29, 0, 30, tzinfo=UTC)
    assert overdue(invoices, now=lambda: fixed) == [...]
```

Test the DST dates of the zones you serve, both directions, and the end of the year for ISO
weeks.

## Summary

- Aware datetimes only: `datetime.now(UTC)`, `ZoneInfo("Region/City")`, never `utcnow()`.
- Gaps and folds do not raise; detect them with `fold` and a round trip through UTC.
- `+ timedelta` on an aware datetime is wall time; elapsed time is computed in UTC.
- Durations with `time.monotonic()`.
- ISO 8601 with an offset on the wire; `isocalendar()` year and week together.
- Inject `now` so the tests can travel in time.
