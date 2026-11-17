# Sidequest: INI config reader

Build `dojo::Config`, the reader of a configuration file, in `config.hpp` and `config.cpp`:

```ini
# the web server
name = demo

[server]
host = 127.0.0.1
port = 8080
```

It grows in four stages: parsing text with errors that say where they are, typed values,
files that include other files, and writing documents back.

- The interface the tests call is fixed by each stage's statement; how you store sections and
  keys, and which helpers you write, is yours.
- Expected failures are values, never exceptions: `std::expected` with an error that says what
  went wrong and where, `std::optional` for something that may be absent.
- A `Config` is a value: copies are independent and two configs with the same sections and
  keys, in the same order, compare equal.
- `cmake -B build && cmake --build build && ctest --test-dir build` builds and runs your own
  tests in `tests/`. The checks run offline with the standard library and GoogleTest only.

The dojo also checks the compiler warnings, clang-format and clang-tidy on your files. When
you are done, `[A]` copies a review prompt: ask an AI about your design.
