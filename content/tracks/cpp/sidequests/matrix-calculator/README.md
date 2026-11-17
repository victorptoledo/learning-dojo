# Sidequest: Matrix calculator

Build `dojo::Matrix`, a small library of matrices of `double`, in `matrix.hpp` and
`matrix.cpp`. It grows in three stages: a value type that keeps its shape consistent, the
arithmetic of matrices, and reading, writing and inverting them.

- The interface the tests call is fixed by each stage's statement; everything behind it is
  yours: how you store the numbers, which helpers you write, how you split the work.
- A `Matrix` is a **value**: copying one copies its numbers, and two matrices with the same
  shape and the same numbers compare equal. Let the standard library own the memory (a
  `std::vector<double>` and the rule of zero) instead of writing `new` and `delete`.
- Failure is reported without exceptions: an operation that cannot be done (shapes that do not
  fit, text that is not a matrix, a singular matrix) returns `std::nullopt` or `false`.
- `cmake -B build && cmake --build build && ctest --test-dir build` builds and runs your own
  tests in `tests/` (the dojo adds its hidden contract tests there when it checks). The checks
  run offline with the standard library and GoogleTest only.

Like every C++ kata, the dojo also checks the compiler warnings, clang-format and clang-tidy
on your files. When you are done, `[A]` copies a review prompt: ask an AI about your design.
