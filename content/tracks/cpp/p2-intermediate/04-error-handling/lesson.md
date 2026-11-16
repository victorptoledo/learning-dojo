# Errors: Exceptions, Exception Safety and std::expected

C++ has two good ways to report failure. **Exceptions** are for errors the immediate caller
usually cannot handle: they unwind the stack (running destructors) to a handler far away.
**`std::expected<T, E>`** (C++23) is for failures that are a normal outcome, which the caller
should look at right away. Choosing between them, and writing code that stays correct while
an exception passes through, is what this lesson is about.

## Exceptions

```cpp
#include <stdexcept>

double ratio(int part, int whole) {
    if (whole == 0) {
        throw std::invalid_argument("ratio: whole is zero");
    }
    return static_cast<double>(part) / whole;
}

try {
    std::println("{}", ratio(1, 0));
} catch (const std::invalid_argument& e) {    // catch by const reference
    std::println(stderr, "bad input: {}", e.what());
} catch (const std::exception& e) {           // more general handlers last
    std::println(stderr, "error: {}", e.what());
}
```

- Throw by value, catch by `const&` (catching by value slices derived exceptions).
- Derive your own from `std::runtime_error` or `std::logic_error`, or use them directly.
- `throw;` inside a `catch` rethrows the current exception unchanged.
- The standard library throws `std::bad_alloc`, `std::out_of_range` (`at`),
  `std::bad_optional_access` (`value`), `std::system_error` and others.

Exceptions cost nothing on the path where nothing is thrown, and a lot when one is thrown.
Use them for exceptional situations, not for control flow in a hot loop.

## Exception safety guarantees

Every function that can fail gives one of these guarantees (Dave Abrahams' terms, used by
the standard library):

| Guarantee | Promise |
| --- | --- |
| **nothrow** | it never fails (`noexcept`): destructors, moves, `swap` |
| **strong** | if it throws, the state is as before the call (commit or roll back) |
| **basic** | if it throws, no leaks and every invariant holds, but the state may have changed |
| none | anything can happen: a bug |

RAII gives the basic guarantee almost for free: destructors release everything during
unwinding. The strong guarantee comes from **doing the work on the side and committing with
operations that cannot throw**:

```cpp
void Inventory::replace_all(std::vector<Item> items) {
    std::map<std::string, Item> fresh;          // build the new state aside (may throw)
    for (auto& item : items) {
        fresh.emplace(item.sku, std::move(item));
    }
    items_.swap(fresh);                         // commit: noexcept
}
```

## noexcept

`noexcept` declares that a function does not throw; if it throws anyway, `std::terminate`
ends the program. Destructors are `noexcept` by default, so **never let an exception escape a
destructor**. Mark moves and `swap` `noexcept` (containers depend on it), and functions that
truly cannot fail. Do not sprinkle it on functions that may throw later.

## std::expected

```cpp
#include <expected>

enum class ParseError { Empty, NotANumber, OutOfRange };

std::expected<int, ParseError> parse_port(std::string_view text) {
    if (text.empty()) {
        return std::unexpected(ParseError::Empty);
    }
    int value = 0;
    const auto [end, error] = std::from_chars(text.data(), text.data() + text.size(), value);
    if (error != std::errc{} || end != text.data() + text.size()) {
        return std::unexpected(ParseError::NotANumber);
    }
    if (value < 1 || value > 65535) {
        return std::unexpected(ParseError::OutOfRange);
    }
    return value;
}

auto port = parse_port(input);
if (!port) {
    report(port.error());
    return;
}
connect(*port);
```

`std::expected<T, E>` holds either a `T` or an `E` inline (no allocation). The error type is
part of the signature, so callers see what can go wrong. C++23 adds monadic chaining:

```cpp
auto url = parse_port(text)
               .and_then(check_allowed)                         // expected<int, E>
               .transform([](int p) { return std::format("http://localhost:{}", p); })
               .value_or("http://localhost:8080");
```

`value()` on an error throws `std::bad_expected_access`; `*` on an error is undefined.

## Which one?

| Use | When |
| --- | --- |
| `std::expected` / `std::optional` | failure is a normal outcome the caller handles now: parsing, validation, lookups |
| exceptions | the caller several levels up handles it, or a constructor cannot build a valid object, or the failure is rare (out of memory, a broken invariant from the outside) |
| `assert` / contracts | a bug in *your* code: a precondition that callers must never break |
| error codes (`std::error_code`) | OS and C APIs, or code that must not use exceptions |

Some codebases (games, embedded, Google's style guide) disable exceptions; there,
`std::expected` and error codes carry everything. Whatever you choose, be consistent in a
module, and never swallow errors silently with an empty `catch (...)`.

## Summary

- Throw by value, catch by `const&`; derive from the standard exceptions.
- Know which guarantee each function gives; RAII gives the basic one, build-then-swap the
  strong one.
- Destructors, moves and swap are `noexcept`; nothing may escape a destructor.
- `std::expected<T, E>` for expected failures, with the error in the type.
- Exceptions for failures handled far away and for failed constructors.

## Further reading

- cppreference, Exceptions: <https://en.cppreference.com/w/cpp/language/exceptions>
- cppreference, std::expected: <https://en.cppreference.com/w/cpp/utility/expected>
- cppreference, noexcept specifier: <https://en.cppreference.com/w/cpp/language/noexcept_spec>
- C++ Core Guidelines, E: Error handling: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-errors>
- isocpp.org FAQ, Exceptions and error handling: <https://isocpp.org/wiki/faq/exceptions>
- Herb Sutter, *Exceptional C++* (exception safety items)
