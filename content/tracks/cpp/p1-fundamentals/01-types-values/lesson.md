# Types, Values, References and Strings

C++ is a language of **values**. A variable *is* the object, not a reference to one
somewhere on a heap: assigning copies it, and passing it to a function copies it unless you
ask for a reference. This one idea explains why C++ is fast, and most of what surprises
people who come from Java, Python, JavaScript or C#.

## Fundamental types

| Type | Typical size | Notes |
| --- | --- | --- |
| `bool` | 1 byte | `true` / `false` |
| `char` | 1 byte | signedness depends on the platform; a byte of text |
| `int` | 4 bytes | at least 16 bits by the standard; use it by default |
| `long long` | 8 bytes | at least 64 bits |
| `std::size_t` | 8 bytes | unsigned, the type of sizes and indices |
| `std::int32_t`, `std::uint64_t` | exact | from `<cstdint>`, when the width matters (files, protocols) |
| `double` | 8 bytes | IEEE 754; `float` is 4 bytes |

Sizes are not fixed by the standard (only minimums), so code that needs an exact width says
so with `<cstdint>`.

### Integer traps

- **Signed overflow is undefined behaviour.** `INT_MAX + 1` is not "it wraps": the compiler
  may assume it never happens and optimise accordingly. Unsigned arithmetic wraps modulo 2ⁿ.
- **Mixing signed and unsigned** converts the signed value: `-1 < 0u` is `false`, because
  `-1` becomes a huge unsigned number. `-Wall -Wextra` warns about many of these.
- **Narrowing**: `int x = 3.9;` silently becomes 3. Brace initialisation refuses it.
- Integer division truncates toward zero: `7 / 2 == 3`, `-7 / 2 == -3`.

## Initialisation

```cpp
int a = 1;          // copy initialisation
int b{2};           // brace (list) initialisation: no narrowing allowed
int c{};            // value-initialised: 0
int d;              // a local int is NOT initialised: reading it is undefined behaviour
auto e = 3.5;       // auto deduces double from the initialiser
std::vector<int> v{1, 2, 3};
```

Prefer `{}` or `auto x = ...`, and always initialise. Uninitialised locals are a classic
source of "works in debug, fails in release".

## const and constexpr

```cpp
const int limit = read_limit();   // cannot change after initialisation
constexpr int kSize = 64;         // a compile-time constant
```

`const` is a promise checked by the compiler, and it spreads: a function that takes
`const std::string&` promises not to modify the argument, and can only call `const` member
functions on it. Make everything `const` that does not need to change.

## References and pointers

A **reference** is another name for an existing object. It must be initialised, cannot be
null and cannot be re-seated:

```cpp
int count = 1;
int& alias = count;     // alias *is* count
alias += 1;             // count == 2

void bump(int& n) { ++n; }      // the caller's variable changes
bump(count);                    // count == 3
```

A **pointer** holds an address. It can be null, can be changed to point elsewhere, and needs
`*` to reach the object:

```cpp
int* p = &count;   // address of count
*p = 10;           // count == 10
p = nullptr;       // points to nothing now; *p would be undefined behaviour
```

Rule of thumb: use a reference when the object must exist, a pointer (or `std::optional`, or
a smart pointer, later) when "nothing" is a valid answer. Raw pointers in modern C++
**observe**; they do not own.

## Value semantics

```cpp
std::vector<int> a{1, 2, 3};
std::vector<int> b = a;   // a full copy: three new ints
b.push_back(4);           // a still has 3 elements
```

Copies are deep by default for standard types. That is safe and often cheap (small objects
live on the stack, copies of large ones can be avoided with references or moves), and it is
why C++ code rarely needs defensive copies or immutability tricks.

## Strings

`std::string` owns a mutable sequence of `char` (bytes, typically UTF-8). Short strings are
stored inside the object itself (the small string optimisation), so they do not allocate.

```cpp
std::string name = "Ada";
name += " Lovelace";
name.size();               // 12 bytes (not characters!)
name.substr(0, 3);         // "Ada", a new string
name.find("Love");         // 4, or std::string::npos when missing
name.starts_with("Ada");   // C++20
```

`std::string_view` (C++17) is a **non-owning view**: a pointer and a length into characters
someone else owns. It is the right parameter type for read-only text, because it accepts a
`std::string`, a string literal or part of a buffer without copying:

```cpp
bool is_greeting(std::string_view text) {
    return text.starts_with("Hello");
}
is_greeting("Hello there");   // no std::string is built
```

The catch: a view must never outlive the characters. Returning a `string_view` into a local
`std::string`, or keeping one to a temporary, is a **dangling view**:

```cpp
std::string_view bad() {
    std::string local = "temporary";
    return local;   // the string dies here; the view points at freed memory
}
```

AddressSanitizer reports this as a `heap-use-after-free` or `stack-use-after-return`.

### Formatting and conversions

```cpp
#include <format>
std::string line = std::format("{} has {} items ({:.1f}%)", "cart", 3, 37.5);
std::to_string(42);           // "42"
std::stoi("42");              // 42, throws on bad input
std::from_chars(...)          // fast, no exceptions, no locale (see the checkpoint)
```

## Undefined behaviour

Some mistakes have no defined result: reading an uninitialised variable, signed overflow,
an out-of-bounds index, dereferencing null or a dangling pointer. The program may crash, work
"by accident", or be optimised into something absurd. You cannot test it away; you avoid it
with good habits and catch it with sanitizers. The dojo compiles katas with
`_GLIBCXX_ASSERTIONS` and ASan/UBSan, so many of these fail loudly with a file and a line.

## Summary

- Variables are values; assignment and by-value parameters copy.
- Initialise everything; prefer `{}` (no narrowing) and `auto`.
- `const` everywhere it fits; `constexpr` for compile-time constants.
- References for "must exist", pointers for "may be null"; raw pointers do not own.
- `std::string` owns text; `std::string_view` borrows it and must not outlive it.
- Signed overflow, uninitialised reads and out-of-bounds access are undefined behaviour.

## Further reading

- cppreference, Fundamental types: <https://en.cppreference.com/w/cpp/language/types>
- cppreference, Initialization: <https://en.cppreference.com/w/cpp/language/initialization>
- cppreference, std::basic_string_view: <https://en.cppreference.com/w/cpp/string/basic_string_view>
- cppreference, Undefined behavior: <https://en.cppreference.com/w/cpp/language/ub>
- C++ Core Guidelines, ES: Expressions and statements: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-expr>
- C++ Core Guidelines, Con: Constants and immutability: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-const>
- Bjarne Stroustrup, *A Tour of C++* (3rd edition), chapters 1 and 10
