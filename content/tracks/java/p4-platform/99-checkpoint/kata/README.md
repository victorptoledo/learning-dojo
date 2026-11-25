# Checkpoint 4: Timesheet Import

Shift workers clock in and out on an app that drops one CSV file per team into an inbox
folder, with wall-clock times in the zone where each person worked. Payroll needs the
**minutes worked per person and ISO week**. Build the import.

Already written: `@Column` (with `required`), the `Entry` record (its constructor refuses a
blank person and an end that is not after the start) and `ImportResult`. You write
`CsvMapper` and `TimesheetImport`.

## 1. `CsvMapper<R extends Record>`: rows to records by annotation

`CsvMapper.of(type)` reads the record **once** and keeps what `map` needs. It refuses, with an
`IllegalArgumentException` that names `Record.component` (for example `Entry.zone`):

- a component without `@Column`;
- a type other than `String`, `int`, `long`, `LocalDate`, `LocalDateTime` and `ZoneId`;
- `required = false` on a primitive (`int` cannot be null);
- two components reading the same column.

`columns()` lists the column names in component order.

`map(row)` takes a `Map<String, String>` from column name to raw text (other columns are
ignored), trims each value and calls the record's canonical constructor. Errors are
`IllegalArgumentException` with exactly these messages:

| Case | Message |
| --- | --- |
| a required value missing or blank | `missing column 'count'` |
| an optional value missing or blank | (the component is `null`) |
| text that does not parse | `column 'at': cannot read '25/11/2026 09:15' as LocalDateTime` |
| the record's constructor refuses | its own message, for example `end must be after start` |

## 2. `TimesheetImport.run(inbox, report)`

1. Read the regular files **directly** in `inbox` whose name ends in `.csv`, **sorted by
   name**. Ignore other files and folders (even a folder called `old.csv`). A missing inbox
   throws `NoSuchFileException` and leaves the old report alone.
2. Each file is UTF-8. Line 1 is the header: column names in **any order**, maybe with a
   byte order mark (`\uFEFF`) in front. If `person`, `zone`, `start` or `end` is missing,
   report `name.csv:1: missing column 'end'` and skip the file.
3. Skip blank lines and lines starting with `#`. Every other line is split on `,` (no quoting)
   and must have as many fields as the header: otherwise `name.csv:5: expected 5 fields, got 3`.
   Map it with `CsvMapper.of(Entry.class)`; a mapping error becomes `name.csv:6: ` followed by
   its message. Line numbers count **every** line of the file, the header being line 1.
4. The minutes of an entry are the **real time** between start and end in its zone: a shift
   from 00:30 to 03:30 lasts 120 minutes on the night the clocks go forward and 240 on the
   night they go back. An entry counts in the ISO week of its start date, written
   `2026-W48` (the **week-based year**, a dash, `W` and two digits).
5. Write the report, replacing any old one:

   ```
   person,week,minutes
   Ada,2026-W48,555
   Ada,2026-W49,60
   Linus,2026-W48,180
   ```

   Sorted by person, then week; just the header when there is nothing. Write it to a temp
   file **in the report's folder** and move it over the report with `ATOMIC_MOVE`, so a reader
   never sees half a report and no temp file is left behind. Create the folder if needed.
6. Return `ImportResult(files read, entries counted, errors in file and line order)`.

> [!TIP]
> Use the JDK only: `Files.list` and `Files.newBufferedReader` in try-with-resources,
> `getRecordComponents()`, the canonical constructor, `InvocationTargetException.getCause()`,
> `Duration.between` on `ZonedDateTime`, `IsoFields.WEEK_BASED_YEAR` and
> `IsoFields.WEEK_OF_WEEK_BASED_YEAR`, and `Files.createTempFile(folder, ...)` with
> `Files.move(..., ATOMIC_MOVE)`. The visible tests use `@TempDir`.
