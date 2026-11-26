# Checkpoint 4: The Meter Readings Import

Every night, smart meters drop CSV files into an inbox. Build the importer in `readings.go`:
read the inbox as an `fs.FS`, add up the energy per meter and **local** day in a time zone
with daylight saving time, and write the report as JSON, atomically. The types and
signatures are declared; the functions and the struct tags are yours.

```text
meter,time,kwh
M1,2026-03-29 00:30,1.25
M1,2026-10-25T00:30:00Z,0.5
```

```go
lisbon, _ := time.LoadLocation("Europe/Lisbon")
report, err := readings.Import(os.DirFS("inbox"), readings.Options{Zone: lisbon})
// err lists the bad rows; report has everything that was good
readings.WriteReport("out", report)
```

| Function or rule | Does |
| --- | --- |
| `ParseEnergy(s)` | Kilowatt-hours with at most three decimals, digits only: `"12"`, `"12.5"`, `"0.001"`. The result is watt-hours (`12.5` is `12500`), so no floats. Anything else (`"-1"`, `"1,5"`, `".5"`, `"1."`, `"1e3"`, a fourth decimal, a number too big) wraps `ErrBadEnergy`. |
| `Energy.String`, `MarshalText` | `12345` is `"12.345"`, `5` is `"0.005"`; in JSON it is that string. |
| files | Only the `.csv` files (any case: `1.CSV` counts) at the **root** of the `fs.FS`, in name order; folders and other files are ignored, and an empty file is fine. |
| header | The first row names the columns, in any order, in any case, with spaces around them and maybe a byte order mark (`﻿`) before the first one. `meter`, `time` and `kwh` are required; other columns are ignored. A file without one of them is skipped with a `*RowError` on line 1 that wraps `ErrMissingColumn` and names the column. |
| `time` | Either an RFC 3339 instant (`2026-10-25T00:30:00Z`, `...+01:00`) or a local time `2006-01-02 15:04` in `Options.Zone` (nil means UTC). A local time that does **not exist** there, because the clocks jumped over it, wraps `ErrBadTime`. |
| bad rows | A bad meter (empty), time, energy or number of fields is a `*RowError{File, Line, Err}` (lines count from 1, header included) and the import goes on with the next row. `Import` returns the report **and** `errors.Join` of every row error. |
| duplicates | The same meter at the same **instant**, however it is written, counts once: the first one read wins and `Report.Duplicates` counts the others. Two readings an hour apart are different, even when both show `01:30` on the clock. |
| `Day` | One per meter and **local calendar date** in the zone: the summed energy, the number of readings, and `Hours`, the length of that local day (23 when clocks go forward, 25 when they go back, otherwise 24). Days are sorted by meter, then date. `Days` is never nil. |
| `WriteReport(dir, r)` | Writes `dir/report.json`, indented with two spaces and ending in a newline, with the field names `days`, `meter`, `date`, `kwh`, `readings`, `hours` and `duplicates`, and `"days": []` for an empty report. It writes a temporary file in the same folder, syncs and closes it, then renames it over `report.json`, so a reader never sees half a report, and leaves no temporary file behind, even on error. Errors wrap the cause (`fs.ErrNotExist` for a missing folder). |

Tools: `fs.ReadDir`, `path.Ext`, `strings.EqualFold`, `encoding/csv` (its `Reader`,
`FieldPos`, `ParseError` and `ErrFieldCount`), `time.ParseInLocation`, `time.Date` with a
day of `d+1`, `Time.In`, `time.DateOnly`, `os.CreateTemp`, `os.Rename`, `json.Encoder` with
`SetIndent`, and `testing/fstest.MapFS` in the tests.

Run the visible test with `go test`. The dojo runs 12 tests: 1 in `readings_test.go` and 11
hidden ones, plus `go vet` and `gofmt` on `readings.go`.
