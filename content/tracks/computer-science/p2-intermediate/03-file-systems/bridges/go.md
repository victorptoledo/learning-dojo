---
from: go
status: draft
concept: "Files and durability in Go"
from_way: "Ignoring the error from Close and assuming the data is safe."
native_way: "os.File.Write goes to the page cache; File.Sync calls fsync; os.Rename is atomic on one file system; Close can report deferred write errors."
sensei_tip: "Check the errors of Sync and Close; for atomic saves, write a temp file with os.CreateTemp in the same directory, Sync, Close, os.Rename, then Sync the directory."
---

| In Go | Underneath |
| --- | --- |
| `f.Write(b)` | into the page cache |
| `f.Sync()` | fsync |
| `os.Rename(a, b)` | atomic replace |
| `bufio.Writer.Flush()` | user-space buffer to the OS |
| `syscall.Stat_t.Ino` | the inode number |
