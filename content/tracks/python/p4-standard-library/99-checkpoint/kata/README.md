# Agenda Import

Three teams export their meetings as CSV files into one `inbox` folder: some from Excel on
Windows, some from tools in São Paulo and Tokyo, some twice. Write `agenda.py` to turn the
folder into one agenda, report every row it could not use, and save the result where a
dashboard reads it at any moment. `Event`, `Rejected`, `ImportResult` and `REQUIRED` are
already written.

## `import_inbox(inbox) -> ImportResult`

**Files.** Every `*.csv` directly inside `inbox` (not in subfolders; other files ignored), in
name order. Files are UTF-8, maybe with a BOM, maybe with `\r\n` endings, with quoted fields
that can contain commas.

| Whole-file problem | `Rejected(file, line, reason)` | Rows kept |
| --- | --- | --- |
| A required column is missing (an empty file too) | `(name, 1, "missing column <first missing in REQUIRED order>")` | none |
| The bytes are not valid UTF-8, anywhere in the file | `(name, 0, "not utf-8")` | none, not even rows before the bad byte |

**Header.** The columns `title`, `date`, `start`, `duration` and `zone` can come in any order,
with spaces around them and in any case; other columns are ignored.

**Rows.** Blank rows are skipped. A row with fewer fields counts the missing ones as empty.
Each row is checked in this order, and the **first** problem is reported as
`Rejected(file, line, reason)`, where `line` is the row's line in the file (the header is line
1; blank lines count, as in an editor). Then the next row is read.

| Field | Valid | Otherwise |
| --- | --- | --- |
| `title` | Anything after normalising to **NFC** and collapsing whitespace runs to one space (no spaces at the ends) is not empty | `"empty title"` |
| `date` | `YYYY-MM-DD` with ASCII digits, and a real date | `"bad date"` |
| `start` | `HH:MM`, 24-hour, two digits each, `00:00` to `23:59` | `"bad time"` |
| `duration` | `"2h"`, `"45m"`, `"1h 30m"`, `"2h15m"`: hours then minutes, each optional, ASCII digits, more than zero | `"bad duration"` |
| `zone` | An IANA zone name such as `Europe/Lisbon` | `"unknown zone"` |
| the local time | Exists exactly once in that zone | `"nonexistent time"` in a DST gap, `"ambiguous time"` in a DST fold |

**Events.** `start` is the local date and time in the row's zone, converted to **UTC**. `end`
is `start` plus the duration of **real time**: a 2-hour night shift that crosses a clock change
still lasts 2 hours. `zone` is the zone name, and `source` the file name.

**Duplicates.** Two rows are the same meeting when they start at the same instant and their
titles have the same key: NFKC, casefold and collapsed whitespace (`"Weekly Sync"` at 10:00 in
Lisbon and `"WEEKLY  sync"` at 05:00 in New York). Keep the first one, in file and line order;
the others are dropped silently.

**Result.** `events` sorted by `start`, then `title`; `rejected` in file and line order.

## `write_agenda(result, target)`

Writes UTF-8 JSON that keeps non-ASCII text readable (`"Reunião"`, not `"Reuni\u00e3o"`):

```json
{"events": [{"title": "...", "start": "2026-11-26T09:00:00+00:00", "end": "...", "zone": "...", "source": "..."}],
 "rejected": [{"file": "b.csv", "line": 4, "reason": "bad time"}]}
```

The dashboard reads `target` at any time, so it must never see half a file: write a temporary
file **in the same folder** and move it over `target` with `os.replace`. If that fails, the
old file stays as it was and the temporary file is removed; the error propagates.

## `weekly_counts(events, zone) -> dict[tuple[int, int], int]`

How many events start in each ISO week, as `{(iso_year, iso_week): count}`, using each event's
**local date in `zone`**. 2027-01-01 is in week 53 of 2026.

Tools: `pathlib`, `csv.reader` with `newline=""`, `encoding="utf-8-sig"`, `unicodedata`,
`re.fullmatch` with `re.ASCII`, `datetime.combine`, `zoneinfo.ZoneInfo`, `fold`,
`astimezone(UTC)`, `json.dumps(ensure_ascii=False)`, `tempfile`, `os.replace`, `isocalendar()`.

Run the visible tests with `uv run pytest`. There are 23 tests: 1 in `tests/test_visible.py`
and 22 hidden ones, plus ruff on `agenda.py`.
