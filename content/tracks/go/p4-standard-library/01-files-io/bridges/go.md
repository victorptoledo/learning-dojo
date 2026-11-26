---
from: go
status: draft
concept: "File APIs since Go 1.16"
from_way: "ioutil.ReadFile, ioutil.WriteFile, ioutil.TempFile and ioutil.ReadDir, filepath.Walk calling os.Lstat on every file, http.Dir for static files, and hand-written checks against ../ in user paths."
native_way: "os.ReadFile, os.WriteFile, os.CreateTemp and os.ReadDir (1.16), filepath.WalkDir with fs.DirEntry (1.16), io/fs and embed (1.16), and os.Root to stay inside a folder (1.24)."
sensei_tip: "io/ioutil is deprecated: every function moved to io or os. go fix can rewrite most calls, and os.Root replaces the strings.HasPrefix(cleaned, base) checks that symbolic links used to defeat."
---

| Older Go | Go 1.27 |
| --- | --- |
| `ioutil.ReadFile` / `WriteFile` | `os.ReadFile` / `os.WriteFile` |
| `ioutil.ReadAll` | `io.ReadAll` |
| `ioutil.TempFile` / `TempDir` | `os.CreateTemp` / `os.MkdirTemp` |
| `ioutil.ReadDir` (returns `[]os.FileInfo`) | `os.ReadDir` (returns `[]fs.DirEntry`) |
| `filepath.Walk` | `filepath.WalkDir` |
| go-bindata and similar generators | `//go:embed` and `embed.FS` |
| `http.Dir("static")` | `http.FS(fsys)` or `http.FileServerFS` |
| `filepath.Clean` + prefix check | `os.OpenRoot(dir)` and `root.Open(name)` |
