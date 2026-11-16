---
from: javascript
status: draft
concept: "Direct access to the file system"
from_way: "In the browser files arrive through <input type=file> or the File System Access API, and paths are URLs."
native_way: "node:fs reads and writes any file the process may access, node:path builds OS paths, and file: URLs bridge the two worlds."
sensei_tip: "new URL(\"./x.json\", import.meta.url) works in both the browser and Node.js; in Node.js, fs functions accept that URL directly."
---

| Browser | Node.js |
| --- | --- |
| `await file.text()` | `await readFile(p, "utf8")` |
| URL strings | OS paths with `node:path`, or `file:` URLs |
| `new URL("./x", import.meta.url)` | The same, plus `fileURLToPath` |
| No real file system | Full access (as far as permissions allow) |
