# Containers, Iterators and Algorithms

The standard library gives you containers that own their elements, iterators that walk them,
and more than a hundred algorithms that work on any range. Knowing which container to pick
and when an iterator becomes invalid is most of what you need.

## The containers you will use

| Container | Is | Use it for |
| --- | --- | --- |
| `std::vector<T>` | a growable contiguous array | **the default**: fast iteration, cache-friendly |
| `std::array<T, N>` | a fixed-size array on the stack | small sizes known at compile time |
| `std::string` | a vector of `char` with text operations | text |
| `std::deque<T>` | blocks of arrays | push and pop at both ends |
| `std::map<K, V>` | a balanced tree, sorted by key | ordered keys, range queries; O(log n) |
| `std::unordered_map<K, V>` | a hash table | fast lookup by key; O(1) average |
| `std::set<T>` / `std::unordered_set<T>` | keys only | membership, distinct values |
| `std::list<T>` | a doubly linked list | splicing and stable iterators (rarely faster) |
| `std::span<T>` | a non-owning view of contiguous elements | parameters: "any array of T" |

Bjarne Stroustrup's advice still holds: use `std::vector` unless you have a reason not to.
Even when the textbook says a linked list is better for inserts in the middle, a vector often
wins because memory is contiguous.

```cpp
std::vector<int> v;
v.reserve(100);         // allocate once if you know the size
v.push_back(1);
v.emplace_back(2);      // builds the element in place
v[0];                   // no bounds check
v.at(5);                // throws std::out_of_range
v.size(); v.empty(); v.back(); v.pop_back();
```

## Maps

```cpp
std::map<std::string, int> ages{{"ada", 36}, {"alan", 41}};
ages["grace"] = 85;                  // inserts or assigns
int n = ages["nobody"];              // operator[] INSERTS a default 0 for a missing key!
if (auto it = ages.find("ada"); it != ages.end()) {
    std::println("{}", it->second);  // it->first is the key
}
ages.contains("alan");               // C++20
ages.try_emplace("ada", 1);          // inserts only if missing; returns {iterator, inserted}
for (const auto& [name, age] : ages) { ... }   // sorted by name
```

`operator[]` on a missing key inserts a value, so it cannot be used on a `const` map and it
silently grows the map; use `find`, `contains` or `at` to read.

**Heterogeneous lookup:** with `std::map<std::string, V, std::less<>>`, `find` accepts a
`std::string_view` without building a temporary `std::string`.

For `unordered_map`, the key needs `std::hash` and `==`. Iteration order is unspecified.

## Iterators

An iterator points at an element; `begin()` is the first, `end()` is **one past the last**.
Algorithms take iterator pairs (or, since C++20, ranges):

```cpp
auto it = std::find(v.begin(), v.end(), 42);
if (it != v.end()) { *it = 0; }
```

### Iterator invalidation

Changing a container can invalidate iterators, references and pointers into it:

| Container | Invalidated by |
| --- | --- |
| `vector`, `string` | an insertion that reallocates (all of them), or insert/erase before the element |
| `deque` | insertion in the middle (all); at the ends, iterators but not references |
| `map`, `set`, `list` | only erasing **that** element |
| `unordered_map` | erasing that element; a rehash invalidates iterators but not references |

The classic bug:

```cpp
for (auto it = v.begin(); it != v.end(); ++it) {
    if (*it < 0) v.erase(it);       // it is invalid now; ++it is undefined behaviour
}
std::erase_if(v, [](int x) { return x < 0; });   // C++20: the right way
```

Holding a reference to `v[0]` and then calling `v.push_back` is the same bug in disguise.

## Algorithms

`<algorithm>` and `<numeric>` hold the loops you would otherwise write by hand. The C++20
`std::ranges::` versions take the whole container and support **projections**:

```cpp
std::ranges::sort(v);                                   // ascending
std::ranges::sort(people, {}, &Person::age);            // by a member (projection)
std::ranges::sort(words, [](const auto& a, const auto& b) {
    return a.size() != b.size() ? a.size() < b.size() : a < b;   // two-level rule
});
auto it = std::ranges::find_if(v, [](int x) { return x > 10; });
auto n = std::ranges::count(v, 0);
bool all = std::ranges::all_of(v, [](int x) { return x >= 0; });
auto [lo, hi] = std::ranges::minmax(v);
int total = std::accumulate(v.begin(), v.end(), 0);     // <numeric>
std::ranges::reverse(v);
auto last = std::ranges::unique(v);                     // after sort: removes duplicates
```

A comparison for sorting must be a **strict weak ordering**: `a < b` style, never `<=`. A
comparator that says `a` comes before `a` is undefined behaviour and can crash `std::sort`.

`std::stable_sort` keeps the order of equal elements; `std::partial_sort` and
`std::nth_element` are cheaper when you only need the top k.

## std::span for parameters

```cpp
double mean(std::span<const double> values);    // accepts vector, array, C array...
mean(my_vector);
mean(std::array{1.0, 2.0});
```

Like `string_view`, a span does not own the elements and must not outlive them.

## Summary

- `std::vector` by default; maps for keyed data (sorted or hashed).
- `map::operator[]` inserts; read with `find`, `contains` or `at`.
- Know what invalidates iterators; use `std::erase_if` instead of erasing in a loop.
- Prefer algorithms (and `std::ranges::`) to hand-written loops; comparators are strict.
- `std::span` and `std::string_view` for non-owning parameters.

## Further reading

- cppreference, Containers library: <https://en.cppreference.com/w/cpp/container>
- cppreference, Iterator invalidation (in each container page and the summary): <https://en.cppreference.com/w/cpp/container#Iterator_invalidation>
- cppreference, Constrained algorithms (std::ranges): <https://en.cppreference.com/w/cpp/algorithm/ranges>
- C++ Core Guidelines, SL.con: Containers: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#SS-con>
- Sean Parent, "C++ Seasoning" (GoingNative 2013): no raw loops
- Nicolai Josuttis, *The C++ Standard Library* (2nd edition)
