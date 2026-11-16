# Resource Handles

Write the members declared in `buffer.hpp`, in `buffer.cpp`.

## Part 1: `Buffer`, the rule of five by hand

`Buffer` owns an array of `int` on the heap through a raw pointer. In real code you would use
`std::vector<int>`; here you write what the vector does for you.

| Member | Does |
| --- | --- |
| `Buffer(size)` | Allocates `size` ints, all 0 (`new int[size]{}`). Size 0 allocates nothing. |
| `~Buffer()` | Frees the array. |
| `Buffer(const Buffer&)` | A deep copy: its own array with the same values. |
| `operator=(const Buffer&)` | Replaces the contents with a deep copy; the old array is freed; `a = a` keeps the data. |
| `Buffer(Buffer&&) noexcept` | Takes the other's array; the other becomes empty (`size() == 0`, `data() == nullptr`). |
| `operator=(Buffer&&) noexcept` | Frees its own array, then takes the other's. |
| `at(index)` | The value, or `std::out_of_range` when `index >= size()`. |
| `swap(other) noexcept` | Exchanges the pointers and sizes. |

## Part 2: `IntList`, ownership with `std::unique_ptr`

Each node owns the next one through `std::unique_ptr<Node>`: there is no `new` or `delete` in
this class. Write `push_front`, `pop_front`, `front` (both throw `std::out_of_range` on an empty
list), `reverse` (relink the nodes in place) and the destructor.

The default destructor would free a node, which frees its `next`, which frees its `next`...
one stack frame per node. A hidden test builds a list of 5 million nodes: free them in a loop.

## What the dojo catches

The tests run under AddressSanitizer: a leak (a missing `delete[]`), a double free (two
buffers that think they own the same array), a use after free and a stack overflow each fail
with the file and line where it happened. 16 hidden tests and the visible one.
