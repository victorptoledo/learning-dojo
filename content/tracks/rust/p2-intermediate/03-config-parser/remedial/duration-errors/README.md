# Duration Errors

Practice for *Config Parser*. `parse_duration(text)` reads a duration such as `"90s"`,
`"5m"` or `"2h"` and returns its length in seconds. Spaces around the text are ignored.
Every failure is a `DurationError`, never a panic:

| Input | Error |
| :--- | :--- |
| empty or blank | `Empty` |
| ends with a digit (`"42"`) | `MissingUnit` |
| ends with a letter other than `s`, `m`, `h` (`"3d"`) | `UnknownUnit('d')` |
| the part before the unit is not a whole number, or the result overflows (`"1.5h"`, `"h"`) | `BadNumber("1.5")`, `BadNumber("")` |

Also implement `Display` for `DurationError`:

- `Empty` → `empty duration`
- `MissingUnit` → `missing unit (use s, m or h)`
- `UnknownUnit('d')` → ``unknown unit `d` ``
- `BadNumber("1.5")` → `` `1.5` is not a whole number ``

`std::error::Error` is already implemented on top of it, so the error works with `?` and
`Box<dyn Error>`. Save a file to run the checks. Passing gives practice XP; it does not
change the kata.
