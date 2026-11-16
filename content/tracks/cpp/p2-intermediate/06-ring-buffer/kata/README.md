# Generic Code

Everything in this kata is a template, so it all lives in the header: edit `generic.hpp`
(there is no `.cpp`).

## Part 1: a concept and generic algorithms

| Name | What it is |
| --- | --- |
| `concept Number<T>` | True for integral and floating-point types, false for `bool` and everything else. |
| `sum(range)` | The total of a range of `Number`s, `T{}` when empty. A range of strings must not even be a valid call: the constraint uses `Number`. |
| `mean(range)` | The average as a `double`, or `std::nullopt` when empty. |
| `max_by(range, proj = {})` | A copy of the element with the largest `std::invoke(proj, element)`; the first one on a tie; `std::nullopt` when empty. |

```cpp
dojo::sum(std::views::iota(1, 101));         // 5050
dojo::max_by(people, &Person::age);          // a member pointer is a projection
dojo::max_by(people, [](const Person& p) { return p.name.size(); });
```

## Part 2: `RingBuffer<T, N>`

A queue of at most `N` values stored in a `std::array<std::optional<T>, N>`: it never
allocates. `push` refuses when full, `push_overwrite` drops the oldest value first, `pop`
returns the oldest (or nothing), `front` throws `std::out_of_range` when empty, and
`for_each(f)` calls `f` oldest first. `capacity()` is `constexpr`. It must work with move-only
types such as `std::unique_ptr<int>`, so move values in and out; never copy them.

14 hidden tests and 2 visible ones, plus the tool checks on `generic.hpp`.
