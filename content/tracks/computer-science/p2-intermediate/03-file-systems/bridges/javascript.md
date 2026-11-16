---
from: javascript
status: draft
concept: "Files and durability in Node.js"
from_way: "Believing fs.writeFile is durable when its promise resolves."
native_way: "writeFile hands data to the OS; fs.fsync or filehandle.sync() forces it; fs.rename is atomic within a file system; async fs calls run on libuv's thread pool."
sensei_tip: "For config or state files, write to a temp path, `await fh.sync()`, `await fs.rename(tmp, path)`, and keep fs work off the event loop."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `fs.promises.writeFile` | into the page cache |
| `filehandle.sync()` | fsync |
| `fs.rename` | atomic replace |
| `fs.watch` | inotify, kqueue or ReadDirectoryChangesW |
| `fs.createWriteStream` | buffered writes with back-pressure |
