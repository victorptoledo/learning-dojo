Configuration files, which may include other files. Add to `config.hpp`:

```cpp
// Two more kinds at the end of ErrorKind:
CannotOpen,    // a file that cannot be read
IncludeCycle,  // an @include that leads back to a file being read

struct LoadError {
    std::filesystem::path file;  // the file the error is in
    std::size_t line = 0;        // counting from 1; 0 when the file itself cannot be opened
    ErrorKind kind{};

    bool operator==(const LoadError&) const = default;
};

// Member of Config: reads a file, following its @include lines.
static std::expected<Config, LoadError> load(const std::filesystem::path& file);
```

- A line `@include <path>` (trimmed, like any line) reads that file **in place**, as if its
  lines were pasted there: the open section goes on into it, and whatever section it opens
  goes on after it. The path is relative to the folder of the file that has the `@include`.
- Every error names the file it is in and the line there: a bad line inside an included file
  is reported in that file, and a key defined twice across files is a `DuplicateKey` where the
  second one is.
- A file that does not exist: `CannotOpen` on the `@include` line, or with line 0 for the file
  given to `load`.
- An `@include` of a file that is still being read (itself, or one that includes it on the
  way) is an `IncludeCycle` on that line. `a/../b.ini` and `b.ini` are the same file:
  compare `std::filesystem::weakly_canonical` paths. Including the same file twice, one after
  the other, is fine.
- `parse` knows nothing about files: it stays as it was.

`std::ifstream` closes its file in its destructor, on every path out of the function: let it
(RAII). Parsing a line and reading a file both need "the open section", so the state of the
reader deserves a small class of its own that `parse` and `load` share.
