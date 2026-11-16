# Performance: Memory Layout, Allocation, Measuring and Profiling

People choose C++ for speed, but speed comes from **design**, not from the language alone.
Modern CPUs are fast at arithmetic and slow at waiting for memory, so most performance work
is about touching less memory, touching it in order, and allocating less. And none of it
matters until you have **measured**.

## Measure first

> Premature optimization is the root of all evil. (Donald Knuth, 1974)

The full quote continues: "yet we should not pass up our opportunities in that critical 3%".
To find that 3%:

1. Build with optimisations (`-O2` or `-O3`, `-DNDEBUG`, `CMAKE_BUILD_TYPE=Release` or
   `RelWithDebInfo`). Debug builds can be 10–50× slower and lie about hot spots.
2. **Profile** the real workload: `perf record` / `perf report` on Linux, Visual Studio's
   profiler or Intel VTune on Windows, Instruments on macOS. Look at where time goes before
   guessing.
3. **Benchmark** the candidate change in isolation with a framework that handles warm-up,
   repetitions and the optimiser (Google Benchmark, nanobench, Catch2's benchmarks), and keep
   the benchmark.

```cpp
static void BM_Lookup(benchmark::State& state) {
    auto table = make_table(state.range(0));
    for (auto _ : state) {
        benchmark::DoNotOptimize(table.find(42));   // keep the call from being removed
    }
}
BENCHMARK(BM_Lookup)->Range(1 << 10, 1 << 20);
```

## Complexity still comes first

A better algorithm beats any micro-optimisation: an O(n²) duplicate check over a million
items is a trillion comparisons; a hash set makes it a million. Know the costs of the
standard containers (vector push_back is amortised O(1), map is O(log n), unordered_map is
O(1) on average with a bad worst case) and pick the right one. The LRU-cache kata is an
example: a list plus a hash map turns every operation into O(1).

## The memory hierarchy

| Level | Typical latency |
| --- | --- |
| register | < 1 ns |
| L1 cache (32–64 KB per core) | ~1 ns |
| L2 cache | ~4 ns |
| L3 cache (shared) | ~15 ns |
| main memory | ~80–100 ns |

Memory moves in **cache lines** of 64 bytes, and the hardware prefetcher is very good at
sequential access. Consequences:

- **Contiguous beats linked.** Iterating a `std::vector<int>` streams cache lines;
  iterating a `std::list<int>` jumps to a random address per node. For most sizes vector
  wins even for inserts in the middle.
- **Structure of arrays** can beat an array of structures when a loop uses only some fields:
  `std::vector<float> xs, ys;` instead of `std::vector<Particle>` with ten fields.
- **Keep hot data small**: reorder members to avoid padding (largest first), use smaller
  types where the range allows, keep rarely used data out of line.
- **False sharing**: two threads writing variables on the same cache line slow each other
  down; pad per-thread counters to `std::hardware_destructive_interference_size`.

## Allocation

A heap allocation costs tens to hundreds of nanoseconds and can contend between threads.

- `reserve` vectors when you know the size; reuse buffers across iterations
  (`clear()` keeps the capacity).
- Pass and return by value with moves, but avoid copies in loops: `for (const auto& x : v)`.
- Small objects live inline: `std::string` (SSO), `std::optional`, `std::variant`,
  `std::array`, `std::inplace_vector` (C++26).
- `std::pmr` (polymorphic memory resources) lets containers use an arena:

```cpp
std::array<std::byte, 64 * 1024> buffer;
std::pmr::monotonic_buffer_resource arena(buffer.data(), buffer.size());
std::pmr::vector<std::pmr::string> names(&arena);   // allocations come from the buffer
```

## What the compiler does for you

At `-O2` the optimiser inlines small functions, unrolls and vectorises loops (SIMD), removes
dead code and keeps values in registers. Help it:

- Keep hot functions visible (in headers, `inline`, or with link-time optimisation `-flto`).
- Avoid aliasing surprises (two pointers that might point to the same memory block
  vectorisation); work on local copies in tight loops.
- Prefer simple loops over containers; avoid virtual calls in the innermost loop.
- `[[likely]]`, `[[unlikely]]` and profile-guided optimisation (`-fprofile-generate` /
  `-fprofile-use`) tell it which branches are hot.

Look at the generated assembly when it matters: [Compiler Explorer](https://godbolt.org/)
shows it line by line.

## Avoid accidental costs

| Smell | Fix |
| --- | --- |
| `std::endl` in a loop (flushes every line) | `'\n'` |
| `const std::string&` parameter called with a literal (builds a string) | `std::string_view` |
| `std::shared_ptr` copies in hot paths (atomic increments) | pass `const T&` or a raw observer |
| exceptions for normal control flow | `std::expected` or a status |
| `std::map<std::string, ...>` with tiny keys looked up millions of times | `unordered_map`, a flat sorted vector or `std::flat_map` (C++23) |
| logging that formats strings that are never printed | check the level first |

## Summary

- Measure: release builds, a profiler, a real benchmark.
- Algorithms and data structures first.
- Memory is the bottleneck: contiguous data, small hot structs, no false sharing.
- Allocate less: reserve, reuse, inline storage, arenas.
- Let the optimiser work, and check its output when it matters.

## Further reading

- Ulrich Drepper, "What Every Programmer Should Know About Memory": <https://people.freebsd.org/~lstewart/articles/cpumemory.pdf>
- cppreference, std::pmr::monotonic_buffer_resource: <https://en.cppreference.com/w/cpp/memory/monotonic_buffer_resource>
- cppreference, std::hardware_destructive_interference_size: <https://en.cppreference.com/w/cpp/thread/hardware_destructive_interference_size>
- Google Benchmark user guide: <https://github.com/google/benchmark/blob/main/docs/user_guide.md>
- Brendan Gregg, Linux perf examples: <https://www.brendangregg.com/perf.html>
- Compiler Explorer: <https://godbolt.org/>
- Chandler Carruth, "Efficiency with Algorithms, Performance with Data Structures" (CppCon 2014)
- Kurt Guntheroth, *Optimized C++* (O'Reilly, 2016)
