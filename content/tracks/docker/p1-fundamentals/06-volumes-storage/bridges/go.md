---
from: go
status: draft
concept: "Persistent state outside the process"
from_way: "A Go service opens files relative to its working directory or a -data flag, and the server's disk keeps them."
native_way: "Point the data path at a mounted volume (DATA_DIR=/data), create the directory with the right owner in the image, and keep the rest of the file system read-only."
sensei_tip: "In a scratch or distroless image there is no writable /tmp unless you mount one; os.CreateTemp fails until you add a tmpfs."
---

| Go | Container storage |
| --- | --- |
| `os.WriteFile(filepath.Join(dir, ...))` | `dir` on a named volume |
| `os.TempDir()` | a tmpfs mount |
| embedding files with `embed` | baked into the image, read-only |
| a `-config` flag pointing at a file | a read-only bind mount |
