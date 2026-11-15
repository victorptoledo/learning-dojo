---
from: go
status: draft
concept: "Files, names and links"
from_way: "os.Rename, os.Remove, os.Symlink, os.Link and filepath.WalkDir."
native_way: "The same system calls through cp, mv, rm, ln and find."
sensei_tip: "Go has no os.Copy: copying is io.Copy between two open files. os.Rename maps straight to rename(2), so the write-temp-then-rename trick works as is."
---

| Go | Linux |
| --- | --- |
| os.Rename(a, b) | mv a b |
| os.RemoveAll(p) | rm -r p |
| os.Symlink(target, link) | ln -s target link |
| os.Link(a, b) | ln a b |
| filepath.WalkDir(dir, fn) | find dir |
