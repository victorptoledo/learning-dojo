# Time with std::chrono: Durations, Clocks, Calendars and Time Zones

Time bugs are expensive and hard to see: a timeout in the wrong unit, a duration measured
with a clock that jumps, a job that runs twice when the clocks go back. `<chrono>` makes most
of them **type errors**. C++11 brought durations and clocks; C++20 added calendars, time
zones and formatting.

## Durations: numbers with units

```cpp
using namespace std::chrono_literals;   // 5s, 250ms, 2h, 3min

std::chrono::milliseconds timeout = 5s;          // OK: exact conversion, 5000 ms
auto total = 1h + 30min;                         // std::chrono::minutes, 90
// std::chrono::seconds s = 1500ms;              // error: would lose precision
auto s = std::chrono::duration_cast<std::chrono::seconds>(1500ms);   // 1 s, truncated
auto f = std::chrono::floor<std::chrono::seconds>(-1500ms);           // -2 s
auto r = std::chrono::round<std::chrono::seconds>(1500ms);            // 2 s (to even)
total.count();                                   // 90, the raw number: only at the edges
```

A `std::chrono::duration<Rep, Period>` is a count and a compile-time ratio. Conversions that
are exact happen silently; lossy ones need `duration_cast` (truncates towards zero), `floor`,
`ceil` or `round`. Pass durations around, not `int`s: `void wait_for(std::chrono::milliseconds)`
accepts `2s` and converts it correctly, and nobody has to guess the unit.

## Time points and clocks

A `time_point<Clock, Duration>` is a duration since the clock's epoch. Subtracting two time
points gives a duration; adding a duration gives a time point.

| Clock | Use it for | Note |
| --- | --- | --- |
| `steady_clock` | measuring elapsed time, timeouts | never goes backwards |
| `system_clock` | dates and timestamps (UTC) | can jump when the time is set (NTP, admin) |
| `high_resolution_clock` | nothing in particular | an alias of one of the other two |

```cpp
const auto start = std::chrono::steady_clock::now();
run_job();
const auto took = std::chrono::steady_clock::now() - start;
std::println("took {}", std::chrono::duration_cast<std::chrono::milliseconds>(took));  // "took 42ms"
```

`std::chrono::sys_seconds` is a `system_clock` time point in seconds (Unix time, without leap
seconds); `sys_days` is one in days, the bridge to the calendar.

## Calendars

```cpp
using namespace std::chrono;

year_month_day date = 2026y / November / 26;        // also 26d / 11 / 2026y
date.ok();                                           // true
(2026y / February / 29).ok();                        // false: not a leap year
auto last_day = 2026y / February / last;             // year_month_day_last: the 28th
weekday wd{sys_days{date}};                          // Thursday
auto next_week = sys_days{date} + weeks{1};          // arithmetic on days
auto month_later = date + months{1};                 // 2026-12-26 (may be !ok(): Jan 31 + 1 month)
auto second_friday = 2026y / March / Friday[2];      // year_month_weekday
```

Calendar types are plain fields; `sys_days{date}` turns a date into a time point, and
`year_month_day{tp}` (after `floor<days>`) turns it back. Adding `months` keeps the day
number, so `January / 31 + months{1}` is February 31, which is `!ok()`: decide what you
want (clamp to the last day, or overflow into March with `sys_days{...}`).

The ISO week (Monday first, week 1 holds the year's first Thursday) is not a type, but it is
three lines: move to the Thursday of the date's week, take that Thursday's year, count weeks
from January 1 of that year.

## Time zones

```cpp
const std::chrono::time_zone* lisbon = std::chrono::locate_zone("Europe/Lisbon");
std::chrono::zoned_time meeting{lisbon, std::chrono::sys_days{2026y / July / 1} + 9h};
std::println("{}", meeting);                        // 2026-07-01 10:00:00 WEST
auto utc = meeting.get_sys_time();                  // back to UTC
auto local = lisbon->to_local(utc);                 // a local_time: a wall clock with no zone
```

`local_time` is a separate type on purpose: "09:00" means nothing until you say *where*, and
the type system stops you from mixing it with UTC. The library reads the IANA **tz
database** (`std::chrono::get_tzdb()`), the same data your operating system uses.

### Gaps and overlaps

On the last Sunday of March, Lisbon jumps from 01:00 straight to 02:00. On the last Sunday
of October, 01:00 to 02:00 happens twice. So a local time can be **nonexistent** (in the gap)
or **ambiguous** (in the overlap):

```cpp
const local_seconds wall = local_days{2026y / October / 25} + 1h + 30min;
lisbon->to_sys(wall);                               // throws ambiguous_local_time
lisbon->to_sys(wall, choose::earliest);             // the first 01:30 (summer time)
lisbon->to_sys(wall, choose::latest);               // the second 01:30 (winter time)

const auto info = lisbon->get_info(local_days{2026y / March / 29} + 1h + 30min);
info.result == local_info::nonexistent;             // true: 01:30 never happens that day
```

Decide per use case: a backup at 02:30 can run at the first valid time after the gap; an
alarm in the overlap usually wants `earliest`. **Never add 24h to get "tomorrow at the same
time"**: across a change that lands an hour off. Add a day to the local date, then convert.

## Formatting and parsing times

```cpp
std::println("{:%F %T}", floor<seconds>(system_clock::now()));   // 2026-11-26 14:03:12
std::println("{:%A %d %B %Y}", sys_days{date});                    // Thursday 26 November 2026
std::println("{:%G-W%V}", sys_days{date});                         // ISO week: 2026-W48
std::println("{}", 90min);                                         // 90min

std::istringstream in("2026-11-26 14:03");
std::chrono::sys_time<std::chrono::minutes> when;
in >> parse("%Y-%m-%d %H:%M", when);                               // fails the stream on bad input
```

Durations print with their unit suffix (`90min`, `1500ms`). `%F` is `%Y-%m-%d`, `%T` is
`%H:%M:%S`, `%R` is `%H:%M`. For strict, locale-free parsing of a fixed format (file names,
protocols), `std::from_chars` on each field plus a `year_month_day::ok()` check is often
simpler and faster than `parse`.

## Summary

- Durations carry units in their type; exact conversions are implicit, lossy ones explicit.
- Measure with `steady_clock`; timestamp with `system_clock`.
- Calendar types (`year_month_day`, `weekday`, `last`) do date arithmetic; `ok()` validates.
- `zoned_time` and `time_zone::to_sys` convert local time with the tz database; handle
  nonexistent and ambiguous local times explicitly.
- Add days to local dates, not 24h to time points.

## Further reading

- cppreference, Date and time library: <https://en.cppreference.com/w/cpp/chrono>
- cppreference, std::chrono::time_zone::to_sys: <https://en.cppreference.com/w/cpp/chrono/time_zone/to_sys>
- Howard Hinnant, "A <chrono> Tutorial" (CppCon 2016)
- Howard Hinnant, the date library that became C++20 chrono: <https://github.com/HowardHinnant/date>
- IANA time zone database: <https://www.iana.org/time-zones>
