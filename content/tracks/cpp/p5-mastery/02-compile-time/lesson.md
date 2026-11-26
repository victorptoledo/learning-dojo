# Compile-Time Programming: constexpr, consteval and Type Traits

C++ can run code **while it compiles**. A value computed at compile time costs nothing at run
time, a mistake found at compile time never reaches production, and a type chosen at compile
time needs no run-time dispatch. Modern C++ made this ordinary C++ instead of template tricks.

## constexpr functions and variables

```cpp
constexpr std::uint64_t factorial(int n) {
    std::uint64_t result = 1;
    for (int i = 2; i <= n; ++i) {
        result *= static_cast<std::uint64_t>(i);
    }
    return result;
}

constexpr auto f10 = factorial(10);     // computed by the compiler
static_assert(f10 == 3'628'800);        // checked by the compiler
int runtime_n = read();
auto fn = factorial(runtime_n);         // the same function, at run time
```

A `constexpr` function **may** run at compile time when its arguments are constants and the
result is needed in a constant expression. Since C++20 almost everything is allowed inside:
loops, local variables, `std::vector` and `std::string` (as long as the memory is freed
before the end of the evaluation), virtual calls, `try` blocks, `std::sort`. C++23 relaxes
`static` and `goto` restrictions further.

**Undefined behaviour is an error in constant evaluation**: an overflow, an out-of-bounds
read or a use after free inside a `constexpr` evaluation fails to compile. `static_assert`
tests of `constexpr` code are therefore a cheap way to prove some code is UB-free.

## consteval and constinit

```cpp
consteval std::uint32_t crc_of(std::string_view text) { /* ... */ }   // must run at compile time
constexpr auto kId = crc_of("orders.created");                       // OK
// crc_of(user_input);                                               // error: not a constant

constinit int counter = 0;      // initialised at compile time (no static-init order problems), still mutable
```

- `consteval` ("immediate function"): every call must be evaluated at compile time. Good for
  validating literals, for example a format string or a regex (that is how `std::format`
  checks `"{}"` against the arguments).
- `constinit`: a global or `static` that must be initialised without running code at start
  time, which avoids the "static initialisation order fiasco".
- `if consteval { ... } else { ... }` (C++23) picks code depending on whether the call is
  being evaluated at compile time.

## Lookup tables at compile time

```cpp
constexpr auto kSquares = [] {
    std::array<int, 16> table{};
    for (int i = 0; i < 16; ++i) {
        table[i] = i * i;
    }
    return table;
}();                                      // an immediately invoked lambda
static_assert(kSquares[7] == 49);
```

The table is baked into the binary's read-only data; no start-up cost.

## Type traits

`<type_traits>` answers questions about types at compile time:

```cpp
static_assert(std::is_nothrow_move_constructible_v<Buffer>);   // a design check
static_assert(sizeof(Packet) == 16, "Packet is part of the wire format");

template <typename T>
void serialize(const T& value) {
    static_assert(std::is_trivially_copyable_v<T>, "serialize needs a trivially copyable type");
    // ... memcpy is fine for such types
}

using Clean = std::remove_cvref_t<const std::string&>;   // std::string
using Big = std::conditional_t<(sizeof(long) == 8), long, long long>;
```

Traits that are questions (`is_*`) give a `bool`; traits that transform (`remove_cvref`,
`add_pointer`, `decay`, `underlying_type`) give a type. Concepts are built on them and read
better for constraints; traits remain the tool for `static_assert` and type computations.

## if constexpr and tag dispatch

```cpp
template <typename T>
std::string to_text(const T& value) {
    if constexpr (std::is_same_v<T, bool>) {
        return value ? "true" : "false";
    } else if constexpr (std::is_arithmetic_v<T>) {
        return std::to_string(value);
    } else if constexpr (requires { value.to_string(); }) {
        return value.to_string();
    } else {
        return std::format("{}", value);
    }
}
```

Older code used SFINAE (`std::enable_if`) and tag types for the same job; today concepts and
`if constexpr` replace almost all of it. You still need to *read* SFINAE in older libraries.

## Where it is going

C++26 adds **static reflection** (`^^T` to get a description of a type and generate code from
its members, for serialisers and enum-to-string without macros), `std::is_within_lifetime`,
and more `constexpr` library functions. Contracts (`pre`, `post`, `contract_assert`) are also
coming in C++26.

## Costs

Compile-time computation moves cost to the build. Large `constexpr` tables or deep template
recursion slow compilation and can hit limits (`-fconstexpr-steps`, `-fconstexpr-loop-limit`).
Use it for values that are genuinely fixed, and for checks.

## Summary

- `constexpr` functions run at compile time or run time; `static_assert` checks results.
- UB is a compile error in constant evaluation, which makes `constexpr` tests strong.
- `consteval` forces compile-time calls; `constinit` forces compile-time initialisation.
- Type traits and `static_assert` document and enforce design assumptions.
- `if constexpr` and concepts replace most SFINAE.

## Further reading

- cppreference, constexpr specifier: <https://en.cppreference.com/w/cpp/language/constexpr>
- cppreference, consteval specifier: <https://en.cppreference.com/w/cpp/language/consteval>
- cppreference, constinit specifier: <https://en.cppreference.com/w/cpp/language/constinit>
- cppreference, Type traits: <https://en.cppreference.com/w/cpp/meta>
- C++ Core Guidelines, Per.11 and Con.5 (compile-time computation): <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#Rper-Comp>
- Jason Turner, "C++ Weekly" constexpr episodes: <https://www.youtube.com/@cppweekly>
- Ben Deane and Jason Turner, "constexpr ALL the Things!" (CppCon 2017)
