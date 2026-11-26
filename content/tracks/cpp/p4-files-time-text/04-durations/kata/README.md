# Durations and Timestamps

Write the functions in `timefmt.cpp` and the `parse` member of the formatter in
`timefmt.hpp` (it is `constexpr`, so it lives in the header).

## Durations

| Name | Does |
| --- | --- |
| `parse_duration(text)` | `"45s"`, `"1h30m"`, `"2d4h5s"`: numbers followed by `d`, `h`, `m` or `s`, in that order, each at most once, no spaces. Errors: `Empty`; `BadNumber` (a unit or a sign or space where digits belong); `BadUnit` (digits with no unit, or another letter); `UnitOrder`; `OutOfRange` when a number or the total does not fit in `std::chrono::seconds`, checked **before** it overflows. |
| `std::format("{}", Elapsed{d})` | The same form back: `"1h30m"`, `"1d"`, `"0s"`, `"-5m"`. Zero units are left out, so `parse_duration` reads it back. |
| `std::format("{:clock}", Elapsed{d})` | Hours (at least two digits, never wrapped into days), minutes and seconds: `"01:30:05"`, `"26:00:00"`, `"-00:00:05"`. |
| any other option | A `std::format_error` from `parse`: a compile error in a literal format string, an exception from `std::vformat`. |

## Timestamps

| Name | Does |
| --- | --- |
| `parse_timestamp(text)` | `"2026-03-29T01:30:00Z"` or `"2026-03-29T02:30:00+01:00"` (also `-HH:MM`) as a `sys_seconds` in UTC. `BadFormat` for anything without that exact shape, including a non-digit in a field; `BadDate` for a date that does not exist (check it with `year_month_day::ok()`); `BadTime` for an hour above 23, a minute or second above 59, or an offset above 23:59. |
| `local_text(when, zone)` | The wall clock in an IANA zone with its abbreviation: `"2026-03-29 02:00 WEST"`. An unknown zone throws `std::runtime_error` (what `std::chrono::locate_zone` already does). |

```cpp
auto took = dojo::parse_duration("1h30m");            // 5400 s
std::println("{} = {:clock}", dojo::Elapsed{*took}, dojo::Elapsed{*took});  // 1h30m = 01:30:00
```

The dojo runs 14 hidden tests and the visible one under AddressSanitizer and UBSan, plus the
tool checks on both files.
