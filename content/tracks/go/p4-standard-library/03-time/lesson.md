# Time: Instants, Durations, Layouts and Zones

Time bugs hide well: they pass every test written in winter and fail twice a year, or only
for users in another country. The `time` package gives you the right tools, and a few rules
keep you out of trouble.

## Instants and durations

- A **`time.Time`** is an instant with nanosecond precision **and a location** used to show
  it. There is no "naive" time in Go.
- A **`time.Duration`** is an `int64` count of nanoseconds. Write it with units:
  `90 * time.Second`, `1500 * time.Millisecond`. `time.Duration(30)` is 30 ns.

```go
start := time.Now()
deadline := start.Add(2 * time.Minute)
elapsed := time.Since(start)                // = time.Now().Sub(start)
if time.Now().After(deadline) { ... }
d, err := time.ParseDuration("1h30m")       // and d.String() == "1h30m0s"
```

Compare instants with `Equal`, `Before` and `After`, not `==`: `==` also compares the
location and the monotonic reading, so the same instant in UTC and in Lisbon is `!=`. For a
map key, normalise first (`t.UTC()`).

## The monotonic clock

The wall clock can jump: NTP corrections, a manual change, a virtual machine resumed after a
pause. `time.Now()` therefore records **two** readings: the wall clock and a monotonic one.
`Sub`, `Since`, `Until`, `Before` and `After` between two such values use the monotonic
reading, so a measured duration is never negative and never off by an hour.

The monotonic reading is dropped by `t.Round(0)`, `t.UTC()`, `t.In(loc)`, by formatting and
by serialisation. Measure with values straight from `time.Now()`, and store or send instants
without worrying about it.

## Layouts: the reference time

Go does not use `%Y-%m-%d`. A layout is **one specific time written the way you want**:

```text
Mon Jan 2 15:04:05 MST 2006     reference time
01/02 03:04:05PM '06 -0700      the same, numbered 1 2 3 4 5 6 7
```

| Part | Write | Part | Write |
| --- | --- | --- | --- |
| year | `2006` | hour (24 h) | `15` |
| month | `01` or `Jan` | hour (12 h) | `03` + `PM` |
| day | `02` | minute | `04` |
| weekday | `Mon` | second | `05`, fractions `.000` |
| zone offset | `-07:00` or `Z07:00` | zone name | `MST` |

Ready-made layouts: `time.RFC3339` (`2006-01-02T15:04:05Z07:00`, the format for APIs and
JSON), `time.RFC3339Nano`, `time.DateOnly` (`2006-01-02`), `time.TimeOnly` (`15:04:05`) and
`time.DateTime` (`2006-01-02 15:04:05`). A wrong layout rarely fails; it gives wrong
numbers. Writing `2006-02-01` swaps day and month without an error.

## Parsing in a location

```go
lisbon, err := time.LoadLocation("Europe/Lisbon")       // IANA name, never "WET"
t1, err := time.Parse(time.RFC3339, "2026-07-01T23:30:00Z") // the offset is in the text
t2, err := time.ParseInLocation("2006-01-02 15:04", "2026-07-02 00:30", lisbon)
t1.Equal(t2)                                              // true: the same instant
```

- `time.Parse` without an offset in the text assumes **UTC**. Wall-clock text from a user or
  a device belongs to a zone: use `ParseInLocation`.
- `LoadLocation` reads the IANA time zone database from the system. Minimal container images
  (distroless, scratch) may not have it: `import _ "time/tzdata"` embeds it in the binary
  (about 450 KB).
- `t.In(loc)` shows the same instant in another zone; it never changes the instant.

## Daylight saving time

In 2026, Lisbon moves its clocks from 01:00 to 02:00 on 29 March, and back from 02:00 to
01:00 on 25 October. Two kinds of local time follow:

- **Gaps**: `2026-03-29 01:30` never happens. `time.Date` and `ParseInLocation` do **not**
  fail; they normalise it to an instant an hour away. To reject it, check
  `t.Format(layout) == input`.
- **Overlaps**: `2026-10-25 01:30` happens twice, an hour apart. Text without an offset is
  ambiguous and Go picks one. That is why logs, APIs and meters should send RFC 3339 with an
  offset or in UTC.

Days are not always 24 hours long:

```go
day := time.Date(2026, 10, 25, 0, 0, 0, 0, lisbon)
next := day.AddDate(0, 0, 1)               // next local midnight: calendar arithmetic
next.Sub(day)                              // 25h0m0s
day.Add(24 * time.Hour)                    // 2026-10-25 23:00: elapsed-time arithmetic
```

`AddDate` and `time.Date` (which normalises `d+1`, month 13 and so on) count **calendar**
units in the location. `Add` counts **elapsed** time. "Every day at 09:00" needs the first;
"30 minutes after the request" needs the second. To group instants by local day, convert
with `In(loc)` and use `t.Format(time.DateOnly)` or `t.Date()` as the key.

## Timers, tickers and tests

- `time.After(d)` and `time.NewTimer(d)` fire once; `time.NewTicker(d)` repeats; call `Stop`
  when you no longer need them. Since Go 1.23, unreferenced timers are garbage collected
  and `Reset` never leaves a stale value in the channel.
- `time.AfterFunc(d, f)` runs `f` on its own goroutine.
- Code that reads the clock is hard to test. Two options: inject it
  (`now func() time.Time`, defaulting to `time.Now`), or run the test in a
  **`testing/synctest`** bubble, where `time.Now`, timers and `time.Sleep` use a fake clock
  that jumps forward whenever every goroutine is blocked.

## Summary

- `time.Time` is an instant with a location; `time.Duration` is nanoseconds, always written
  with a unit; compare instants with `Equal`, and key maps by `t.UTC()`.
- `time.Now` carries a monotonic reading, so `Since` and `Sub` measure safely.
- Layouts are the reference time `2006-01-02 15:04:05 -07:00`; use the named constants.
- Parse wall-clock text with `ParseInLocation` and an IANA zone; embed `time/tzdata` in
  minimal images; prefer RFC 3339 with an offset on the wire.
- Daylight saving creates gaps (normalised silently) and overlaps (ambiguous); `AddDate`
  counts calendar days, `Add` counts elapsed time.
- Inject the clock or use `testing/synctest` to test time-dependent code.

## Further reading

- Package time: <https://pkg.go.dev/time>
- Package time, "Monotonic Clocks": <https://pkg.go.dev/time#hdr-Monotonic_Clocks>
- Testing concurrent code with testing/synctest (Go blog): <https://go.dev/blog/synctest>
- IANA time zone database: <https://www.iana.org/time-zones>
- roadmap.sh, Go topic "Time": <https://roadmap.sh/golang>
