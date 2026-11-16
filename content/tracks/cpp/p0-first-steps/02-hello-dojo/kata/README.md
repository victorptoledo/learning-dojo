# Hello, Dojo

Write the two functions declared in `greet.hpp`, in `greet.cpp`.

| Function | Returns |
| --- | --- |
| `std::string greet(std::string_view name)` | `"Hello, <name>!"`. Spaces and tabs around the name are removed; a blank name (empty or only spaces) becomes `stranger`. Spaces inside the name stay. |
| `std::string initials(std::string_view full_name)` | The first letter of each word, in upper case: `"grace brewster hopper"` gives `"GBH"`. Words are separated by any amount of white space; blank text gives `""`. |

```cpp
dojo::greet("Ada");                      // "Hello, Ada!"
dojo::greet("  Grace \t");               // "Hello, Grace!"
dojo::greet("   ");                      // "Hello, stranger!"
dojo::initials("  alan   mathison turing "); // "AMT"
```

Tools: `std::string_view` (`remove_prefix`, `remove_suffix`, `empty`), `std::isspace` and
`std::toupper` from `<cctype>` (cast a `char` to `unsigned char` before you pass it), and
`std::format` from `<format>`.

## How the dojo checks a C++ kata

Each function still throws `std::logic_error("todo: ...")`; the panel shows those tests as
"not written yet". Save a file and the dojo, in a container:

1. compiles every `.cpp` with **GCC 16** in C++23 mode (`-Wall -Wextra -Wpedantic -Wshadow`);
2. runs the **GoogleTest** tests (the visible one in `tests/visible_test.cpp` plus 8 hidden
   ones) under **AddressSanitizer** and **UndefinedBehaviorSanitizer**, so a use after free,
   a leak, a signed overflow or an index out of range fails the test that caused it;
3. adds one test for the compiler warnings in your files, one **clang-format** test and one
   **clang-tidy** test per file. The kata's `.clang-format` and `.clang-tidy` hold the same
   rules, so VS Code's C/C++ extension formats on save the same way.

To build and run the visible test on your own machine (optional; needs CMake and a C++23
compiler, and downloads GoogleTest once):

```bash
cmake -B build
cmake --build build
ctest --test-dir build --output-on-failure
```
