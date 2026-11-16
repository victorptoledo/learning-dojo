# Templates and Concepts

A **template** is a pattern the compiler stamps out for each set of types you use it with.
`std::vector<int>` and `std::vector<std::string>` are two different classes generated from one
template, each as fast as hand-written code. **Concepts** (C++20) say what a template needs
from its types, so a mistake produces one clear error instead of pages of instantiation noise.

## Function templates

```cpp
template <typename T>
T largest(const T& a, const T& b) {
    return a < b ? b : a;
}

largest(3, 7);                    // T = int, deduced from the arguments
largest<double>(3, 7.5);          // explicit: both converted to double
largest(std::string("a"), std::string("b"));
```

Templates are compiled when they are **instantiated**, with the real types, which is why
their definitions live in headers: every translation unit that uses `largest<int>` must see
its body.

The short form with `auto` parameters (C++20) is a template too:

```cpp
auto twice(auto x) { return x + x; }     // same as template <typename T> auto twice(T x)
```

## Class templates

```cpp
template <typename T, std::size_t N>
class StaticStack {
public:
    bool push(T value) {
        if (size_ == N) {
            return false;
        }
        items_[size_++] = std::move(value);
        return true;
    }
    std::size_t size() const { return size_; }
    static constexpr std::size_t capacity() { return N; }

private:
    std::array<T, N> items_{};
    std::size_t size_ = 0;
};

StaticStack<int, 8> stack;       // N is a non-type template parameter
std::pair p{1, 2.5};             // class template argument deduction (CTAD): pair<int, double>
```

## Concepts

Without constraints, a template accepts any type and fails deep inside its body:

```cpp
largest(std::vector<int>{}, std::vector<int>{});   // fine: vectors have <
struct Point { int x, y; };
largest(Point{}, Point{});   // error: no operator< ... (inside largest, in a long message)
```

A **concept** is a named compile-time predicate on types:

```cpp
#include <concepts>

template <typename T>
concept Number = (std::integral<T> || std::floating_point<T>) && !std::same_as<T, bool>;

template <typename T>
concept Shape = requires(const T& s) {          // a requires-expression
    { s.area() } -> std::convertible_to<double>;
    { s.name() } -> std::convertible_to<std::string_view>;
};
```

Use them in any of these equivalent forms:

```cpp
template <Number T> T square(T x) { return x * x; }
template <typename T> requires Number<T> T cube(T x) { return x * x * x; }
Number auto half(Number auto x) { return x / 2; }
```

Now `square(std::string{})` fails at the call site with "constraints not satisfied: Number".
The standard library provides many: `std::integral`, `std::floating_point`,
`std::same_as`, `std::convertible_to`, `std::equality_comparable`, `std::totally_ordered`,
`std::invocable<F, Args...>`, `std::predicate`, and range concepts such as
`std::ranges::input_range`.

Concepts also choose between overloads: the most constrained candidate wins.

## Generic algorithms over ranges

```cpp
template <std::ranges::input_range R>
    requires Number<std::ranges::range_value_t<R>>
auto sum(R&& values) {
    std::ranges::range_value_t<R> total{};
    for (const auto& v : values) {
        total += v;
    }
    return total;
}

sum(std::vector{1, 2, 3});           // 6
sum(std::views::iota(1, 101));       // 5050
```

A **projection** lets the caller say which part of each element matters; `std::invoke` calls
member pointers and callables alike:

```cpp
template <std::ranges::forward_range R, typename Proj = std::identity>
auto max_by(R&& range, Proj proj = {}) {
    auto best = std::ranges::begin(range);
    for (auto it = best; it != std::ranges::end(range); ++it) {
        if (std::invoke(proj, *best) < std::invoke(proj, *it)) best = it;
    }
    return *best;      // (assumes a non-empty range)
}
max_by(people, &Person::age);
```

## if constexpr and variadic templates

`if constexpr` keeps only the branch whose condition is true at compile time; the discarded
branch is not instantiated, so it may use operations the type lacks:

```cpp
template <typename T>
std::string describe(const T& value) {
    if constexpr (std::same_as<T, std::string>) {
        return value;
    } else if constexpr (std::integral<T>) {
        return std::to_string(value);
    } else {
        static_assert(false, "describe: unsupported type");   // fine in a discarded branch (P2593)
    }
}
```

**Variadic templates** take any number of arguments; a fold expression combines them:

```cpp
template <typename... Ts>
auto sum_all(Ts... values) { return (values + ... + 0); }
sum_all(1, 2, 3);   // 6
```

## Costs and habits

- Each instantiation is separate code: templates can grow binaries and compile times.
- Errors come from instantiation; concepts move them to the call site.
- Keep templates in headers; keep non-generic code in `.cpp` files.
- Prefer standard concepts to hand-written ones, and write concepts for **meaningful**
  requirements ("a Shape"), not for every single operation.

## Summary

- Templates generate code per type; their bodies must be visible (headers).
- Class templates take types and values (`RingBuffer<T, N>`); CTAD deduces arguments.
- Concepts name requirements; they give clear errors and choose overloads.
- Write range algorithms with `std::ranges` concepts, `range_value_t` and projections.
- `if constexpr` picks code per type at compile time; fold expressions handle parameter packs.

## Further reading

- cppreference, Templates: <https://en.cppreference.com/w/cpp/language/templates>
- cppreference, Constraints and concepts: <https://en.cppreference.com/w/cpp/language/constraints>
- cppreference, Concepts library: <https://en.cppreference.com/w/cpp/concepts>
- cppreference, if constexpr: <https://en.cppreference.com/w/cpp/language/if#Constexpr_if>
- C++ Core Guidelines, T: Templates and generic programming: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-templates>
- David Vandevoorde, Nicolai Josuttis and Douglas Gregor, *C++ Templates: The Complete Guide* (2nd edition)
- Bjarne Stroustrup, *A Tour of C++* (3rd edition), chapters 7 and 8
