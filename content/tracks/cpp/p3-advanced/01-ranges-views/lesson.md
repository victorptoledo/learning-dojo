# Ranges, Views, Projections and Generators

C++20 ranges let you write data processing as a pipeline: *take the lines, parse them, keep
the errors, count them by service*. Algorithms take whole containers, **views** transform
data lazily without copying it, and C++23 adds `std::ranges::to` to collect results and
`std::generator` to write lazy sequences as ordinary functions.

## Ranges and range algorithms

A **range** is anything with `begin()` and `end()`: containers, arrays, string views, views.
The `std::ranges::` algorithms take a range directly and accept a **projection**:

```cpp
std::vector<Person> people = load();
std::ranges::sort(people, {}, &Person::age);                     // {} = std::ranges::less
auto oldest = std::ranges::max_element(people, {}, &Person::age);
bool any_minor = std::ranges::any_of(people, [](int a) { return a < 18; }, &Person::age);
```

They also catch mistakes the old ones allowed: `std::ranges::sort(list)` does not compile
for a `std::list` (it needs random access), with a message that says so.

## Views: lazy, non-owning pipelines

A view is a cheap object that describes a sequence computed on demand. Adaptors compose with
`|`:

```cpp
#include <ranges>

std::vector<int> values{5, 3, 8, 1, 9, 2};

auto big_squares = values
    | std::views::filter([](int x) { return x > 2; })
    | std::views::transform([](int x) { return x * x; })
    | std::views::take(3);

for (int v : big_squares) { std::println("{}", v); }   // 25 9 64: computed while looping
```

Nothing happens until the loop pulls a value; no intermediate vector is built. Useful
adaptors:

| Adaptor | Gives |
| --- | --- |
| `filter(pred)`, `transform(f)` | the elements that pass, the results of `f` |
| `take(n)`, `drop(n)`, `take_while`, `drop_while` | a prefix or the rest |
| `reverse`, `keys`, `values`, `elements<I>` | reversed order; keys or values of a map |
| `split(delim)`, `join`, `join_with` (C++23) | split into subranges; flatten |
| `enumerate`, `zip`, `zip_transform` (C++23) | index and element; elements side by side |
| `chunk(n)`, `slide(n)`, `adjacent<n>`, `stride(n)` (C++23) | groups, sliding windows, pairs |
| `iota(a, b)`, `repeat(x)`, `single(x)`, `empty<T>` | generated sequences |

### Collecting with ranges::to (C++23)

```cpp
auto names = people
    | std::views::filter([](const Person& p) { return p.active; })
    | std::views::transform(&Person::name)
    | std::ranges::to<std::vector>();          // std::vector<std::string>

auto distinct = words | std::ranges::to<std::set>();
```

### Views do not own, and some are not const-iterable

Views refer to the range they adapt. Returning a view over a local container, or over a
temporary, dangles:

```cpp
auto evens() {
    std::vector<int> v{1, 2, 3, 4};
    return v | std::views::filter(is_even);   // v dies here: the view dangles
}
```

Return a container (`| std::ranges::to<std::vector>()`) or take the source as a parameter the
caller keeps alive. The library protects some cases: algorithms called on a temporary return
`std::ranges::dangling` instead of an iterator, and owning views (`std::views::all` of an
rvalue gives `owning_view`) keep a moved-in container alive.

`filter_view` caches its first element, so iterating it may modify it: a `const` filter view
cannot be iterated. Pass views by value (they are cheap) or as forwarding references.

## Generators (C++23)

`std::generator<T>` turns a function into a lazy range with `co_yield`:

```cpp
#include <generator>

std::generator<std::string_view> lines(std::string_view text) {
    while (!text.empty()) {
        const auto nl = text.find('\n');
        co_yield text.substr(0, nl);              // hand one line to the loop, then pause
        text.remove_prefix(nl == std::string_view::npos ? text.size() : nl + 1);
    }
}

for (std::string_view line : lines(file_contents)) { ... }
auto parsed = lines(text) | std::views::transform(parse) | std::ranges::to<std::vector>();
```

The function runs until the next `co_yield`, then waits until the loop asks for another
value. Generators are input ranges: they can be iterated once. The function's state lives in
a heap-allocated coroutine frame (often optimised away).

## Choosing between a loop, an algorithm and a pipeline

- A named algorithm (`std::ranges::count_if`) says what you mean better than a loop.
- A pipeline shines for "filter, transform, collect" without intermediate containers.
- A plain loop is fine when the body has several effects or early exits; do not force
  everything into views.
- Long pipelines with many lambdas can compile slowly and produce long error messages; give
  lambdas names when they are reused.

## Summary

- `std::ranges::` algorithms take ranges and projections.
- Views are lazy and non-owning; compose them with `|`.
- `std::ranges::to` collects a pipeline into a container.
- A view must not outlive the data it shows; return containers from functions.
- `std::generator` writes lazy sequences with `co_yield`.

## Further reading

- cppreference, Ranges library: <https://en.cppreference.com/w/cpp/ranges>
- cppreference, std::ranges::to: <https://en.cppreference.com/w/cpp/ranges/to>
- cppreference, std::generator: <https://en.cppreference.com/w/cpp/coroutine/generator>
- cppreference, Constrained algorithms: <https://en.cppreference.com/w/cpp/algorithm/ranges>
- Eric Niebler, "Range v3" and the original ranges proposal (P0896): <https://wg21.link/p0896>
- Barry Revzin, "A Plan for C++23 Ranges" (P2214): <https://wg21.link/p2214>
