---
from: python
status: draft
concept: "Modern file handling"
from_way: "os.path.join and string paths, open() without an encoding, f.readlines() on big files, line.split(',') for CSV, and writing the final file in place."
native_way: "pathlib.Path with /, explicit encoding= (UTF-8 mode is the default from 3.15), iterating the file object, csv.DictReader with newline='', and a temp file plus os.replace."
sensei_tip: "UTF-8 mode in 3.15 fixes your defaults, not your inputs: an export from an old system is still cp1252 until you say otherwise."
---

| Older habit | Python 3.15 |
| --- | --- |
| `os.path.join(base, "a.csv")` | `base / "a.csv"` |
| `open(path).read()` | `path.read_text(encoding="utf-8")` |
| `os.listdir(folder)` | `sorted(folder.glob("*.csv"))` |
| `line.split(",")` | `csv.DictReader(f)` |
| `if os.path.exists(p): open(p)` | `try: open(p) ... except FileNotFoundError` |
| `json.dump(data, open("out.json", "w"))` | temp file in the same folder, then `os.replace` |
| a `tests/data` folder you clean by hand | pytest's `tmp_path` |
