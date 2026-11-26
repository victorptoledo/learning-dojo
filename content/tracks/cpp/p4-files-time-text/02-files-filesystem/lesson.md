# Files, Streams and std::filesystem

Files are where programs meet the outside world, and the outside world fails: files vanish,
disks fill up, permissions change, power goes out mid-write. C++ gives you two tools: **file
streams** (`<fstream>`) to read and write contents, and **`std::filesystem`** (C++17) to work
with paths, directories and metadata.

## File streams are RAII objects

```cpp
#include <fstream>

std::ofstream out("report.txt");            // opens (creates or truncates)
if (!out) {
    throw std::runtime_error("cannot open report.txt");
}
out << std::format("{} rows\n", rows);
// no close(): the destructor flushes and closes on every way out of the scope
```

Open modes combine with `|`: `std::ios::app` (append), `std::ios::binary` (no newline
translation on Windows), `std::ios::trunc`, `std::ios::in | std::ios::out`.

A stream does not throw by default. It sets **state flags** (`failbit`, `badbit`, `eofbit`),
and `if (stream)` asks "did everything so far succeed?". Check after opening, and check after
writing something that matters, because a full disk shows up as a failed write or a failed
close.

## Reading lines, and the eof() trap

```cpp
std::ifstream in(path);
std::string line;
while (std::getline(in, line)) {      // the read is the condition
    handle(line);
}
if (in.bad()) { /* a real I/O error, not just the end */ }
```

`while (!in.eof()) { std::getline(in, line); ... }` is the classic bug: `eof` only becomes
true **after** a read fails at the end, so the loop body runs once more with a stale or empty
line. Make the read itself the loop condition.

Lines from Windows files end in `"\r"` after `getline`; strip it when the input may come from
anywhere.

Reading a whole file at once:

```cpp
std::string read_all(const std::filesystem::path& path) {
    std::ifstream in(path, std::ios::binary);
    if (!in) {
        throw std::runtime_error(std::format("cannot open {}", path.string()));
    }
    return {std::istreambuf_iterator<char>(in), std::istreambuf_iterator<char>()};
}
```

For binary data use `std::ios::binary` with `read`/`write` on `char` buffers, and serialise
fields explicitly: writing a `struct` with `write(reinterpret_cast<const char*>(&s), sizeof s)`
sends padding and depends on the machine's byte order.

## std::filesystem::path

```cpp
namespace fs = std::filesystem;

fs::path dir = "/var/log/app";
fs::path file = dir / "2026-11-26.log";       // the right separator on every OS
file.filename();      // "2026-11-26.log"
file.stem();          // "2026-11-26"
file.extension();     // ".log"
file.parent_path();   // "/var/log/app"
file.replace_extension(".gz");
```

A `path` is just a name: building one touches no disk. Concatenating strings with `"/"`
breaks on Windows and on doubled separators; `operator/` does not. `path::string()` gives the
native narrow string; on Windows the native form is wide (`path::native()` is a `std::wstring`).

## Asking the disk

```cpp
fs::exists(p);  fs::is_regular_file(p);  fs::is_directory(p);
fs::file_size(p);  fs::last_write_time(p);
fs::create_directories(p);   // like mkdir -p
fs::remove(p);  fs::remove_all(p);  fs::rename(from, to);  fs::copy_file(from, to);

for (const fs::directory_entry& entry : fs::directory_iterator(dir)) {
    if (entry.is_regular_file() && entry.path().extension() == ".log") {
        total += entry.file_size();   // the entry may have cached this from the listing
    }
}
// fs::recursive_directory_iterator walks sub-folders too
```

The **order** of a directory listing is unspecified: sort the entries when order matters.

## Exceptions or error codes

Almost every `std::filesystem` function comes in two forms:

```cpp
fs::remove(p);                          // throws fs::filesystem_error on failure
std::error_code ec;
fs::remove(p, ec);                      // never throws; check ec afterwards
if (ec) { log(std::format("cannot remove {}: {}", p.string(), ec.message())); }

fs::directory_iterator it(dir, fs::directory_options::skip_permission_denied, ec);
```

Use the exception form when failure means "this program cannot go on", and the `error_code`
form when failure is a normal outcome you handle on the spot, like a file another process
already deleted. The `filesystem_error` carries both paths and the `std::error_code`.

## Check, then act, is a race

```cpp
if (fs::exists(path)) {      // true now...
    std::ifstream in(path);  // ...but another process may delete it in between
}
```

This is a **time-of-check to time-of-use** (TOCTOU) race. The file system is shared state
that other processes change at any moment, so no check stays true. Do the operation and
handle its failure instead: open the stream and test it, call `remove(p, ec)` and accept
"no such file". In security-sensitive code, TOCTOU lets an attacker swap a file for a
symbolic link between the check and the use.

## Replacing a file atomically

Writing a file in place leaves a half-written file if the program crashes or the power goes
out. The standard pattern is to write elsewhere and rename:

```cpp
void write_atomically(const fs::path& target, std::string_view contents) {
    fs::path temp = target;
    temp += ".tmp";                          // same directory, same file system
    {
        std::ofstream out(temp, std::ios::binary | std::ios::trunc);
        out.write(contents.data(), static_cast<std::streamsize>(contents.size()));
        out.close();                         // close to flush, then check
        if (!out) {
            throw std::runtime_error(std::format("cannot write {}", temp.string()));
        }
    }
    fs::rename(temp, target);                // replaces target in one step on POSIX
}
```

On POSIX, `rename` within one file system is atomic: readers see the old file or the new
one, never a mix. The temporary file must be in the **same directory** (a rename across file
systems is a copy). For durability across power loss, production code also calls `fsync` on
the file and the directory, which the standard library does not expose.

## Summary

- File streams are RAII: they close in the destructor. They report failure through state
  flags, so check them after opening and after important writes.
- Loop with `while (std::getline(in, line))`, never on `eof()`.
- Build paths with `std::filesystem::path` and `/`; sort directory listings yourself.
- Use the `std::error_code` overloads when failure is expected, exceptions when it is fatal.
- Checking then acting is a race: act and handle the failure.
- Replace files atomically: write a temporary file in the same directory, then `rename`.

## Further reading

- cppreference, Filesystem library: <https://en.cppreference.com/w/cpp/filesystem>
- cppreference, std::basic_fstream: <https://en.cppreference.com/w/cpp/io/basic_fstream>
- cppreference, std::filesystem::rename: <https://en.cppreference.com/w/cpp/filesystem/rename>
- CWE-367, Time-of-check Time-of-use race condition: <https://cwe.mitre.org/data/definitions/367.html>
- Dan Luu, "Files are hard": <https://danluu.com/file-consistency/>
