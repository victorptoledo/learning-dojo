# Files, io and bufio

Most Go programs read or write something: a configuration file, a CSV export, a request
body, a log. The standard library handles all of them with a few small interfaces. Learn
those, and files, network connections, compressed streams and test buffers all look the
same to your code.

## io.Reader and io.Writer, again

You met them in the interfaces lesson:

```go
type Reader interface{ Read(p []byte) (n int, err error) }
type Writer interface{ Write(p []byte) (n int, err error) }
```

The contract of `Read` is worth knowing exactly: it fills **up to** `len(p)` bytes, returns
how many with `n`, and returns `io.EOF` when the data is over. It may return `n > 0` **and**
an error in the same call, so always use the `n` bytes before you look at the error. You
rarely call `Read` yourself; helpers do it right:

| Helper | Does |
| --- | --- |
| `io.ReadAll(r)` | everything into memory (only for input you know is small) |
| `io.Copy(w, r)` | streams from r to w in 32 KiB chunks, returns the bytes copied |
| `io.LimitReader(r, n)` | stops after n bytes (`http.MaxBytesReader` does the same for bodies) |
| `io.TeeReader(r, w)` | everything read from r is also written to w (hash while you copy) |
| `io.MultiWriter(a, b)` | one write goes to both |
| `io.MultiReader(a, b)` | reads a, then b |
| `io.ReadFull(r, buf)` | exactly `len(buf)` bytes, or `io.ErrUnexpectedEOF` |

Because they compose, "copy a file to stdout while computing its SHA-256" is three lines:

```go
h := sha256.New()
n, err := io.Copy(os.Stdout, io.TeeReader(f, h))
fmt.Printf("%d bytes, sha256 %x\n", n, h.Sum(nil))
```

## Opening, reading and closing files

```go
data, err := os.ReadFile("config.json")        // small files: all at once
err = os.WriteFile("out.txt", data, 0o644)     // create or truncate, then write

f, err := os.Open("big.log")                   // read only
if err != nil {
	return fmt.Errorf("open log: %w", err)      // the error already names the path
}
defer f.Close()
```

`os.Open` errors are `*fs.PathError`, and `errors.Is(err, fs.ErrNotExist)` (or
`fs.ErrPermission`) tells you why, on every operating system. For writing, `os.Create`
truncates, and `os.OpenFile(name, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0o644)` appends.

**The Close error of a file you write matters.** Data may sit in the operating system's
buffers, and some file systems report a failed write only on `Close`. A deferred `Close`
whose error is thrown away can hide a half-written file:

```go
func save(name string, data []byte) (err error) {
	f, err := os.Create(name)
	if err != nil {
		return err
	}
	defer func() {
		if cerr := f.Close(); cerr != nil && err == nil {
			err = cerr
		}
	}()
	_, err = f.Write(data)
	return err
}
```

For files you only read, `defer f.Close()` is fine.

## Atomic replacement: write, sync, rename

If a program crashes halfway through `os.WriteFile("report.json", ...)`, readers find a
truncated report. The standard fix: write a **temporary file in the same folder**, make sure
it is on disk, then rename it over the old one. On one file system, `os.Rename` replaces the
file in one step, so a reader sees the old version or the new one, never half of one.

```go
tmp, err := os.CreateTemp(dir, ".report-*.json")  // unique name, same folder
// ... write to tmp ...
tmp.Sync()                                          // flush to the disk, not just the OS
tmp.Close()                                         // check this error
os.Rename(tmp.Name(), filepath.Join(dir, "report.json"))
// on any error before the rename: os.Remove(tmp.Name())
```

The temporary file must be in the same folder: `/tmp` may be another device, where rename
fails.

## bufio: fewer system calls, and lines

Every `Read` or `Write` on an `*os.File` is a system call. `bufio` puts a buffer in front:

```go
sc := bufio.NewScanner(f)
for sc.Scan() {
	line := sc.Text()          // without the newline; also handles \r\n
	...
}
if err := sc.Err(); err != nil { // the loop ends on EOF *or* on an error
	return fmt.Errorf("read %s: %w", name, err)
}
```

- `Scanner` reads lines by default; `sc.Split(bufio.ScanWords)` or `ScanRunes` changes the
  unit.
- A line longer than 64 KiB stops the scan with `bufio.ErrTooLong`. Raise the limit with
  `sc.Buffer(make([]byte, 0, 1024*1024), 1024*1024)`, or use `bufio.Reader.ReadString('\n')`
  when lines have no bound.
- `bufio.NewWriter(f)` batches small writes. **Call `Flush`** before closing, or the last
  4 KiB never reach the file. Forgetting it is the classic bufio bug.

## Paths: filepath and path

- `path/filepath` handles paths of the operating system: `filepath.Join("out", "a.json")`
  uses `\` on Windows. Use it for files on disk.
- `path` always uses `/`. Use it for URLs and for names inside an `fs.FS`.
- `filepath.WalkDir(root, fn)` visits a tree, cheaper than the older `Walk` because it does
  not stat every file. Return `filepath.SkipDir` from `fn` to skip a folder.

## io/fs: file systems as values

Go 1.16 added `fs.FS`, a read-only file tree as an interface:

```go
type FS interface{ Open(name string) (fs.File, error) }
```

Functions that take an `fs.FS` work with any tree:

| Value | Tree |
| --- | --- |
| `os.DirFS("inbox")` | a folder on disk |
| `embed.FS` from `//go:embed templates/*` | files compiled into the binary |
| `zip.Reader` | the inside of a zip file |
| `fstest.MapFS{"a.csv": {Data: []byte("...")}}` | a map, for tests |

The helpers `fs.ReadFile`, `fs.ReadDir` (sorted by name), `fs.Glob` and `fs.WalkDir` work on
any of them. Names in an `fs.FS` always use `/`, never start with `/` and never contain `..`.
Taking an `fs.FS` instead of a folder name is the file-tree version of "accept an
`io.Reader`": your code is easier to test and to reuse.

## os.Root: staying inside a folder

A program that opens `filepath.Join(base, userInput)` can be tricked by `../../etc/passwd`
or by a symbolic link that points outside. Go 1.24 added `os.Root`:

```go
root, err := os.OpenRoot("uploads")
f, err := root.Open(name)   // fails if name escapes uploads, even through a symlink
```

Use it whenever part of a path comes from outside the program.

## Summary

- Learn the `Read` contract, then use helpers: `io.Copy`, `io.ReadAll`, `LimitReader`,
  `TeeReader`, `MultiWriter`.
- `os.ReadFile` and `WriteFile` for small files; `Open` plus `defer Close` for streams; check
  the `Close` error of files you write.
- Replace files atomically: temporary file in the same folder, `Sync`, `Close`, `Rename`.
- `bufio.Scanner` for lines (check `Err`, mind the 64 KiB limit); `bufio.Writer` needs
  `Flush`.
- `filepath` for disk paths, `path` for slash paths; accept `fs.FS` for trees; `os.Root`
  for paths that come from users.

## Further reading

- Package io: <https://pkg.go.dev/io>
- Package bufio: <https://pkg.go.dev/bufio>
- Package io/fs: <https://pkg.go.dev/io/fs>
- Traversal-resistant file APIs (Go blog, os.Root): <https://go.dev/blog/osroot>
- roadmap.sh, Go topic "File I/O": <https://roadmap.sh/golang>
