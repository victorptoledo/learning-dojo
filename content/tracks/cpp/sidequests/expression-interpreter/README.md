# Sidequest: Expression interpreter

Build an interpreter of formulas such as `2 * max(x, 3) ^ 2 - rate`, in `expr.hpp` and
`expr.cpp`. It grows in four stages: a parser that builds a syntax tree, an evaluator with
variables and functions, a simplifier that rewrites the tree, and a batch of formulas
evaluated on several threads.

- The tree is yours to design, around one idea: a node is a `std::variant` of the kinds of
  node, and each operation (printing, evaluating, simplifying) is a visitor, an overload set
  passed to `std::visit`. No base class, no `virtual`: compare it with the polymorphism lesson.
- A node owns its children (`std::unique_ptr`, `std::vector`): the tree frees itself and moves
  as a whole.
- Every error is a value (`std::expected`) that says what went wrong and where in the text.
- `cmake -B build && cmake --build build && ctest --test-dir build` builds and runs your own
  tests in `tests/`. The checks run offline with the standard library and GoogleTest only;
  the last stage runs everything under ThreadSanitizer.

The dojo also checks the compiler warnings, clang-format and clang-tidy on your files. When
you are done, `[A]` copies a review prompt: ask an AI about your design.
