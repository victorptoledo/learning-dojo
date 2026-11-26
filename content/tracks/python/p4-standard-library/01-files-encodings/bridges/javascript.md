---
from: javascript
status: draft
concept: "Files, paths and text I/O"
from_way: "node:fs/promises, node:path, readline or streams for big files, a CSV package from npm, JSON.parse and JSON.stringify."
native_way: "Blocking file calls by default (wrap them in asyncio.to_thread inside async code), pathlib, iterating a file, and csv and json in the standard library."
sensei_tip: "Python's file API is synchronous; in an asyncio program, move heavy file work to a thread instead of blocking the event loop."
---

| Node.js | Python 3.15 |
| --- | --- |
| `path.join(dir, "a.csv")` | `dir / "a.csv"` |
| `await fs.readFile(p, "utf8")` | `p.read_text(encoding="utf-8")` |
| `readline.createInterface({ input })` | `for line in open(p, encoding="utf-8"):` |
| `JSON.stringify(data, null, 2)` | `json.dumps(data, indent=2)` |
| `csv-parse` from npm | `csv.DictReader` |
| `fs.rename(tmp, target)` | `os.replace(tmp, target)` |
| `Date` in JSON becomes a string silently | `json.dumps` raises `TypeError` for `datetime`: convert it |
