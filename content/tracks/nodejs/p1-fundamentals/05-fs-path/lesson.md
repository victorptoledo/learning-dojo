# The File System, Paths and URLs

`node:fs` gives you the operating system's file API, and `node:path` builds paths that work on
Windows, macOS and Linux. Both are simple, and both have traps: relative paths that depend on
where the process was started, half-written files after a crash, and error codes you must
check.

## Three styles of the same API

```ts
import { readFileSync } from "node:fs";              // synchronous: blocks the thread
import { readFile } from "node:fs";                  // error-first callback
import { readFile as read } from "node:fs/promises"; // promises: use this one

const text = await read("notes.txt", "utf8");
```

Use `node:fs/promises` by default. The `*Sync` functions are fine **at startup** (reading
configuration before the server listens) and in small scripts, but never while serving
requests: they block the event loop for the whole I/O.

## Files

```ts
import { readFile, writeFile, appendFile, copyFile, rename, rm, stat } from "node:fs/promises";

const json = JSON.parse(await readFile("data.json", "utf8"));  // with an encoding: a string
const bytes = await readFile("photo.jpg");                      // without: a Buffer
await writeFile("out.txt", "hello\n");                          // creates or replaces
await appendFile("app.log", "started\n");
await writeFile("new.txt", "x", { flag: "wx" });                // fails if it exists (EEXIST)
await rm("tmp", { recursive: true, force: true });              // rm -rf

const info = await stat("data.json");
info.size;            // bytes
info.isFile();        // or isDirectory(), isSymbolicLink() (with lstat)
info.mtime;           // last modified
```

`readFile` loads the whole file into memory. For files that can be large, use streams
(phase 2).

### Folders

```ts
import { mkdir, readdir, glob } from "node:fs/promises";

await mkdir("build/assets", { recursive: true });           // mkdir -p, no error if it exists
const names = await readdir("src");                          // ["a.ts", "lib", ...]
const entries = await readdir("src", { withFileTypes: true });
for (const entry of entries) {
  console.log(entry.name, entry.isDirectory() ? "folder" : entry.isFile() ? "file" : "other");
}
const all = await readdir("src", { recursive: true });       // every path below src
for await (const file of glob("src/**/*.ts")) console.log(file);
```

`withFileTypes` returns `Dirent` objects, so you know the type of each entry without an extra
`stat` call. A symbolic link is reported as a link, neither a file nor a folder.

## Errors have codes

File system errors carry a **`code`** from the operating system. Check the code, not the
message:

| Code | Meaning |
| --- | --- |
| `ENOENT` | No such file or folder |
| `EEXIST` | Already exists (`mkdir` without `recursive`, flag `wx`) |
| `EACCES` / `EPERM` | Permission denied |
| `ENOTDIR` / `EISDIR` | A file where a folder was expected, or the reverse |
| `EMFILE` | Too many open files |

```ts
async function readOptional(file: string): Promise<string | undefined> {
  try {
    return await readFile(file, "utf8");
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") {
      return undefined;              // expected: the file is optional
    }
    throw error;                     // anything else is a real problem
  }
}
```

Avoid "check, then act": `if (existsSync(f)) await readFile(f)` can still fail if the file
disappears in between. Just try the operation and handle its error.

## Atomic writes

`writeFile` truncates the file and then writes. If the process dies in between, the file is
empty or cut. To replace a file safely, write a temporary file **in the same folder** and
`rename` it: on the same file system a rename is atomic, so readers see the old file or the
new one, never half of it.

```ts
async function writeAtomic(file: string, content: string) {
  const temporary = `${file}.${process.pid}.tmp`;
  await writeFile(temporary, content);
  await rename(temporary, file);
}
```

## File handles and watching

`open` returns a `FileHandle` for many reads or writes on one file. Close it, ideally with
`await using`, which closes it even when an error is thrown:

```ts
import { open, watch } from "node:fs/promises";

{
  await using file = await open("big.log", "r");
  const { bytesRead } = await file.read(Buffer.alloc(100), 0, 100, 0);   // the first 100 bytes
}   // closed here

for await (const event of watch("config", { signal })) {
  console.log(event.eventType, event.filename);  // "change" or "rename"
}
```

`fs.watch` uses the operating system's notifications and differs between platforms (events
can come twice, or without a file name); libraries such as chokidar smooth that over.

## Paths

Never build paths with `+ "/" +`. `node:path` uses the right separator and cleans paths up:

```ts
import path from "node:path";

path.join("data", "2026", "../report.csv");   // "data/report.csv" ("data\\report.csv" on Windows)
path.resolve("data");                          // an absolute path, from process.cwd()
path.basename("/a/b/file.tar.gz");             // "file.tar.gz"
path.extname("file.tar.gz");                   // ".gz"
path.dirname("/a/b/file.txt");                 // "/a/b"
path.relative("/a/b", "/a/c/d");               // "../c/d"
path.parse("/a/b/file.txt");                   // { root, dir, base, ext, name }
path.posix.join("a", "b");                     // always "a/b", e.g. for URLs or zip entries
```

### Relative to what?

A relative path like `"./data.json"` is resolved against `process.cwd()`, the folder the
process was **started from**, not the folder of your code. For files that ship with your
code, build the path from the module's location:

```ts
const schemaPath = path.join(import.meta.dirname, "schema.json");   // next to this module
const sameThing = new URL("./schema.json", import.meta.url);         // a file: URL
```

`import.meta.url` is a `file:` URL. Most fs functions accept a URL directly; convert with
`fileURLToPath(url)` and `pathToFileURL(path)` from `node:url` when you need the other form.

## Summary

- Use `node:fs/promises`; `*Sync` only at startup or in scripts.
- `readdir` with `withFileTypes`, `recursive` and `glob` list folders; `mkdir` with
  `recursive` makes them.
- Check `error.code` (`ENOENT`, `EACCES` ...); try and handle instead of checking first.
- Replace files atomically: write a temporary file, then `rename`.
- Build paths with `node:path`, relative to `import.meta.dirname` for files that ship with the
  code.

## Further reading

- Node.js API, "File system": <https://nodejs.org/api/fs.html>
- Node.js API, "Promises API" of fs: <https://nodejs.org/api/fs.html#promises-api>
- Node.js API, "Path": <https://nodejs.org/api/path.html>
- Node.js API, "Common system errors": <https://nodejs.org/api/errors.html#common-system-errors>
- Node.js API, `import.meta.dirname`: <https://nodejs.org/api/esm.html#importmetadirname>
- Node.js, "Working with file descriptors in Node.js": <https://nodejs.org/en/learn/manipulating-files/working-with-file-descriptors-in-nodejs>
- `rename(2)`, Linux manual page (atomic replacement): <https://man7.org/linux/man-pages/man2/rename.2.html>
