# Backup Rotator

A database is backed up into one folder as `<name>-YYYYMMDD-HHMM.bak`, named after the
**local** wall time where the server runs. Write the four functions in `retention.cpp` that
decide which backups to keep and remove the rest.

## `scan(dir, zone)`

| Case | Result |
| --- | --- |
| `dir` does not exist or is not a folder | `ScanError::NotADirectory` |
| `dir` cannot be listed | `ScanError::Unreadable` |
| a regular file named like a backup | a `Backup`: its path, `taken` (UTC) and `day` (the local date) |
| a local time that happens twice (the night the clocks go back) | the **earlier** of the two moments |
| a local time that never happens (the night the clocks go forward) | skipped |
| any other regular file, including a name with a date or time that does not exist | skipped |
| sub-folders and other entries | ignored (not even skipped) |

A backup name has at least one character before `-YYYYMMDD-HHMM`, digits only in the fields,
an hour up to 23, a minute up to 59, a real date, and the extension `.bak` (lower case).
`backups` comes newest first; `skipped` is sorted by path. Nothing here throws.

## `plan(backups, policy)`

Walk the backups newest first. A backup is kept by a rule when it is the newest backup of its
bucket and the rule has not filled its quota yet:

| Rule | Bucket | Quota |
| --- | --- | --- |
| `daily` | the local day | `policy.daily` days that have backups (not calendar days) |
| `weekly` | the ISO week: Monday to Sunday, counted in its ISO year (2025-12-29 is in week 1 of 2026) | `policy.weekly` |
| `monthly` | the month | `policy.monthly` |

`reason` lists every rule that keeps the backup, in the order `daily+weekly+monthly`. Every
other backup goes to `remove`. Both lists are newest first, whatever order the input had. A
quota of 0 turns the rule off.

## `report(plan)` and `apply(dir, plan)`

```text
keep   db-20261126-1500.bak  daily+weekly+monthly
keep   db-20261125-0300.bak  daily
remove db-20261126-0300.bak
2 kept, 1 removed
```

The action is padded to 6 columns, a kept file's name to the longest kept name, and only file
names appear. `apply` removes each file in `plan.remove` (one that is already gone is fine,
and is not counted), stops at the first real error and returns it as a `std::error_code`
(without writing the log), and otherwise replaces `dir / "retention.log"` with the report
through a temporary file in the same folder and `std::filesystem::rename`. No temporary file
is left behind. It returns how many files it removed.

The dojo runs 15 hidden tests and the visible one under AddressSanitizer and UBSan, plus the
tool checks on both files. The tests use real folders and the `Europe/Lisbon` and
`Asia/Tokyo` time zones.
