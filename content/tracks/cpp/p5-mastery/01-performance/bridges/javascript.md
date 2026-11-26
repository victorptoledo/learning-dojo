---
from: javascript
status: draft
concept: "Performance"
from_way: "V8 optimises hot functions with hidden classes and inline caches; typed arrays keep numbers contiguous; DevTools profile."
native_way: "The compiler optimises ahead of time; every struct is a hidden class that never changes; vectors are like typed arrays for any type."
sensei_tip: "Typed arrays are V8's way to get what std::vector gives every type: contiguous, unboxed data."
---

| JavaScript / TypeScript | C++ |
| --- | --- |
| Chrome DevTools profiler | perf, VTune, Instruments |
| benchmark.js / tinybench | Google Benchmark |
| `Float64Array` | `std::vector<double>` |
| hidden classes | fixed struct layout |
| avoid megamorphic calls | avoid virtual calls in hot loops |
| GC pauses | no GC: allocation cost is up to you |
