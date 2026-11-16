# Functions, Parameters, Lambdas and std::optional

Functions in C++ are free: they do not need a class. What needs care is **how arguments are
passed**, because a parameter is a copy unless you say otherwise. This lesson covers
parameter passing, return values, overloading, lambdas, `enum class`, structured bindings and
`std::optional` for results that may be missing.

## Control flow at a glance

```cpp
if (const auto pos = text.find(':'); pos != std::string_view::npos) {   // if with initialiser
    key = text.substr(0, pos);
}

for (const auto& item : items) { ... }        // range-for: by const reference
for (std::size_t i = 0; i < v.size(); ++i) { ... }
while (queue_not_empty()) { ... }

switch (level) {
    case Level::Warn:
    case Level::Error:
        alert();
        break;               // without break, control falls through
    default:
        break;
}
```

`if`, `switch` and range-`for` accept an initialiser (C++17/20), which keeps helper variables
in the smallest scope.

## Passing parameters

The C++ Core Guidelines (F.15–F.21) give a short decision table:

| Parameter is | Pass as | Example |
| --- | --- | --- |
| cheap to copy (int, double, pointer, `string_view`, `span`) | by value | `int area(int w, int h)` |
| read-only and expensive to copy | `const T&` | `void print(const Report& r)` |
| changed by the function (an "out" or "in-out") | `T&` | `void normalise(std::vector<double>& v)` |
| kept by the function (stored in a member) | by value, then `std::move` | `void set_name(std::string n) { name_ = std::move(n); }` |

Return values instead of using out-parameters: returning a large object is cheap because of
**copy elision** (the result is built directly in the caller's variable) and moves.

```cpp
std::vector<std::string> split(std::string_view text);   // good: return the result
void split(std::string_view text, std::vector<std::string>& out);   // older style
```

To return several values, return a struct (or a `std::pair`/`std::tuple`) and unpack it with
**structured bindings**:

```cpp
struct MinMax { int min; int max; };
MinMax bounds(std::span<const int> values);

const auto [lo, hi] = bounds(values);
for (const auto& [name, score] : scores) { ... }    // works for maps too
```

## Overloading and default arguments

Several functions may share a name if their parameter types differ; the compiler picks the
best match at compile time:

```cpp
double area(double radius);
double area(double width, double height);
std::string pad(std::string_view text, std::size_t width, char fill = ' ');
```

Overload resolution is subtle with conversions; keep overload sets small and obvious.

## enum class

```cpp
enum class Level { Debug, Info, Warn, Error };

Level l = Level::Warn;
if (l >= Level::Warn) { ... }          // comparisons work within the enum
int n = static_cast<int>(l);           // no implicit conversion to int
```

Prefer `enum class` to the old `enum`: its names do not leak into the enclosing scope and it
does not silently convert to `int`.

## Lambdas

A lambda is an unnamed function object, written where it is used:

```cpp
std::vector<int> v{5, 2, 8, 1};
std::ranges::sort(v, [](int a, int b) { return a > b; });    // descending

int threshold = 3;
auto above = [threshold](int x) { return x > threshold; };   // captures a copy
auto count = std::ranges::count_if(v, above);

int total = 0;
std::ranges::for_each(v, [&total](int x) { total += x; });   // captures by reference
```

| Capture | Means |
| --- | --- |
| `[x]` | copy of `x`, taken when the lambda is created |
| `[&x]` | reference to `x`: the lambda must not outlive it |
| `[=]` / `[&]` | everything used, by copy / by reference (be explicit in longer code) |
| `[this]` | the current object, by pointer |
| `[p = std::move(ptr)]` | an init-capture: a new variable, here moved in |

A lambda that captures by reference and is stored (in a callback, a thread, a
`std::function`) after the captured variables die is a dangling reference: capture by copy
when the lambda escapes.

`std::function<int(int)>` can hold any callable with that signature, at the cost of a
possible allocation and an indirect call; templates (`auto` parameters, later) avoid that
cost.

## std::optional

A function that may have no answer should say so in its type, instead of returning `-1`,
`nullptr` or an empty string by convention:

```cpp
#include <optional>

std::optional<int> parse_port(std::string_view text) {
    int value = 0;
    const auto [end, error] = std::from_chars(text.data(), text.data() + text.size(), value);
    if (error != std::errc{} || end != text.data() + text.size() || value <= 0 || value > 65535) {
        return std::nullopt;
    }
    return value;
}

if (const auto port = parse_port(input)) {     // contextual bool: has a value?
    connect(*port);                            // * reads it (undefined if empty!)
}
int p = parse_port(input).value_or(8080);      // a default
parse_port(input).value();                     // throws std::bad_optional_access if empty
```

`std::optional<T>` stores the `T` inline (no allocation). C++23 adds monadic helpers:
`opt.transform(f)`, `opt.and_then(f)`, `opt.or_else(f)`.

## Summary

- Small types by value, big read-only ones by `const&`, outputs by return value.
- Structured bindings unpack structs, pairs and map entries.
- `enum class` for named constants that do not leak or convert.
- Lambdas: capture by copy when the lambda outlives the scope.
- `std::optional<T>` for "a T or nothing"; check before `*`.

## Further reading

- C++ Core Guidelines, F: Functions (parameter passing F.15–F.21): <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-functions>
- cppreference, Lambda expressions: <https://en.cppreference.com/w/cpp/language/lambda>
- cppreference, std::optional: <https://en.cppreference.com/w/cpp/utility/optional>
- cppreference, Structured binding declaration: <https://en.cppreference.com/w/cpp/language/structured_binding>
- cppreference, Enumeration declaration: <https://en.cppreference.com/w/cpp/language/enum>
- Scott Meyers, *Effective Modern C++*, items 31–34 (lambdas)
