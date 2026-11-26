# Files, Paths, Encodings, CSV and JSON

Almost every program reads or writes files: configuration, imports, exports, reports. Python's
standard library does all of it with a few modules: `pathlib` for paths, `open()` for text and
bytes, `csv` and `json` for the two formats you meet every week, and `os`, `shutil` and
`tempfile` for moving files safely. The rules that matter are few, and each one prevents a
classic production bug.

## Paths with pathlib

A `Path` is a location, not a file: it may not exist yet.

```python
from pathlib import Path

inbox = Path("imports")                 # relative to the current working directory
report = inbox / "2026-11.csv"          # / joins with the right separator on every system
report.name, report.stem, report.suffix # ('2026-11.csv', '2026-11', '.csv')
report.parent                           # Path('imports')
report.with_suffix(".json")             # Path('imports/2026-11.json')
here = Path(__file__).resolve().parent  # the folder of this module, not of the shell
```

Never build paths by gluing strings with `"/"` or `"\\"`. And remember that a relative path
depends on where the program was **started**: data that ships with your code is found from
`Path(__file__)` (or `importlib.resources`), not from `"data/rules.json"`.

**Listing.** `inbox.iterdir()`, `inbox.glob("*.csv")` and `inbox.rglob("*.csv")` return
paths in **whatever order the file system gives**, which differs between machines. Sort them
when the order can change the result: `sorted(inbox.glob("*.csv"))`.

**Staying inside a folder.** A name from outside (an upload, a URL) can contain `..`. Check
the resolved path:

```python
target = (root / user_name).resolve()
if not target.is_relative_to(root.resolve()):
    raise ValueError("path escapes the upload folder")
```

## Text is bytes plus an encoding

A file holds bytes. Text mode decodes them with an encoding. Up to Python 3.14, leaving it
out meant the **locale's** encoding: UTF-8 on Linux and macOS, but often `cp1252` on Windows,
so the same script worked on the server and broke on a laptop. Python 3.15 turns on UTF-8 mode
by default (PEP 686), which ends most of that, but the file's real encoding is still a fact
about the file, not about your interpreter: a legacy export may be `cp1252` or `latin-1`, and
your code may run on an older Python or with `PYTHONUTF8=0`. Always pass `encoding=` (the
`-X warn_default_encoding` flag reports the calls that do not):

```python
text = path.read_text(encoding="utf-8")
path.write_text(report, encoding="utf-8")

with path.open(encoding="utf-8") as lines:   # a file object is an iterator of lines
    for number, line in enumerate(lines, start=1):
        handle(number, line.rstrip("\n"))
```

The loop reads one line at a time, so it works for a 10 GB file; `read_text()` and
`readlines()` load everything. The `with` block closes the file even when `handle` raises.

| Problem | What you see | Fix |
| --- | --- | --- |
| Wrong encoding | `UnicodeDecodeError`, or silent mojibake (`Ã£` for `ã`) | Name the real encoding of the source |
| A **BOM** from Excel or Notepad | The first column is called `"﻿name"` | `encoding="utf-8-sig"` strips it (and reads plain UTF-8 too) |
| Bad bytes you must survive | One broken line stops the import | `errors="replace"` (or `"backslashreplace"`), and log it |

Bytes are their own type: `path.read_bytes()`, `open(path, "rb")`, and `data.decode("utf-8")`
/ `text.encode("utf-8")` to cross the line. `len("ã")` is 1 but `len("ã".encode())` is 2.

## Newlines

Text mode turns `\r\n` and `\r` into `\n` when reading, and writes `\n` as the platform's
line ending (`\r\n` on Windows). Pass `newline=""` to switch the translation off, which the
`csv` module needs, or `newline="\n"` to always write Unix endings.

## CSV: always through the csv module

Splitting on commas breaks on the first quoted field (`"Lisbon, Portugal"`). The `csv` module
handles quotes, embedded commas and newlines:

```python
import csv

with path.open(encoding="utf-8-sig", newline="") as source:
    for row in csv.DictReader(source):        # the header row gives the keys
        rows.append(Order(id=row["id"], total=Decimal(row["total"])))

with out.open("w", encoding="utf-8", newline="") as target:
    writer = csv.DictWriter(target, fieldnames=["id", "total"])
    writer.writeheader()
    writer.writerows({"id": o.id, "total": str(o.total)} for o in orders)
```

`DictReader` makes the column order irrelevant, gives `None` for missing trailing fields,
and its `line_num` says where you are (count rows yourself if fields can contain newlines).
Every value is a `str`: convert and validate each one.

## JSON

```python
import json

config = json.loads(path.read_text(encoding="utf-8"))
text = json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True)
```

- JSON has no dates, `Decimal`s, sets or tuples. `json.dumps` raises `TypeError` for them:
  convert first (`isoformat()`, `str(amount)`), or pass `default=` a converter.
- `ensure_ascii=False` keeps `ã` readable instead of `ã`. Both are valid JSON.
- `sort_keys=True` and a fixed `indent` make the output stable, so diffs and tests stay clean.
- For many records, **JSON Lines** (one JSON document per line) can be streamed like any text
  file.

## Writing safely: temp file, then os.replace

A reader that opens a report while you are writing it sees half a file, and a crash halfway
leaves a broken one. Write to a temporary file **in the same folder**, then rename it over the
target. `os.replace` is atomic on the same file system: readers see the old file or the new
one, never a mix.

```python
import os, tempfile

def write_atomic(target: Path, text: str) -> None:
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=target.parent,
                                     prefix=f".{target.name}.", delete=False) as tmp:
        tmp.write(text)
    try:
        os.replace(tmp.name, target)
    except BaseException:
        os.unlink(tmp.name)
        raise
```

The same folder matters: `/tmp` may be another file system, where a rename becomes a copy.
For more durability (power loss), call `tmp.flush()` and `os.fsync(tmp.fileno())` before the
replace. `shutil.copy2`, `shutil.move` and `shutil.rmtree` cover the rest, and
`path.unlink(missing_ok=True)` deletes without a race.

## Errors you should expect

`FileNotFoundError`, `PermissionError`, `IsADirectoryError` and `FileExistsError` are all
subclasses of `OSError`. Catch the specific one where you can act on it ("the inbox is
empty"), and let the others propagate. Do not check `path.exists()` and then open: the file
can vanish in between. Open, and handle the error.

## Testing file code

pytest's `tmp_path` fixture gives each test a fresh folder, so tests use real files and never
touch yours:

```python
def test_import_reads_every_csv(tmp_path):
    (tmp_path / "b.csv").write_text("id,total\n2,5\n", encoding="utf-8")
    (tmp_path / "a.csv").write_text("﻿id,total\n1,3\n", encoding="utf-8")
    assert [o.id for o in import_inbox(tmp_path)] == ["1", "2"]
```

## Summary

- `pathlib.Path` with `/`, and sorted listings when order matters.
- Always pass `encoding=`; `utf-8-sig` for files that may have a BOM; `newline=""` for csv.
- Iterate over a file to stream it; `with` closes it.
- `csv.DictReader` and `DictWriter`, never `split(",")`.
- JSON needs conversions for dates and decimals; sort keys for stable output.
- Temp file in the same folder plus `os.replace` for writes that readers can trust.
