# RAII and Smart Pointers

**RAII** (Resource Acquisition Is Initialization) is the most important idea in C++: tie
every resource to the lifetime of an object. The constructor acquires it, the destructor
releases it, and because C++ runs destructors at a known point (the end of the scope, even
when an exception passes through), the resource can never leak. It is the idea Rust's
ownership grew from, and it works for memory, files, locks, sockets and transactions alike.

## Object lifetime: the stack and the heap

```cpp
void work() {
    std::string name = "ada";                 // automatic storage: dies at the }
    auto* raw = new std::string("grace");     // dynamic storage: lives until delete
    delete raw;                               // you must remember this, exactly once
}                                             // ~string runs for name here
```

Automatic objects (locals, members, temporaries) are destroyed in reverse order of
construction, deterministically. Dynamic objects (`new`) live until someone calls `delete`.
Every bug of manual memory management comes from that "someone":

| Bug | What happened |
| --- | --- |
| **leak** | nobody called `delete` (an early `return`, an exception, a forgotten path) |
| **double free** | two owners both called `delete` |
| **use after free** | someone kept a pointer after the owner deleted it |
| **mismatched delete** | `new[]` freed with `delete` (or the other way round) |

## RAII

```cpp
class File {
public:
    explicit File(const char* path) : handle_(std::fopen(path, "r")) {
        if (handle_ == nullptr) {
            throw std::runtime_error("cannot open file");
        }
    }
    ~File() { std::fclose(handle_); }          // always runs, even during an exception
    File(const File&) = delete;                // one owner: no copies
    File& operator=(const File&) = delete;

    std::FILE* get() const { return handle_; }

private:
    std::FILE* handle_;
};

void read_config() {
    File config("app.conf");
    parse(config.get());      // if parse throws, ~File still closes the file
}
```

The standard library is RAII all the way down: `std::vector` and `std::string` own memory,
`std::fstream` owns a file, `std::lock_guard` owns a mutex lock, `std::jthread` joins its
thread. Write classes whose members are such objects and the rule of zero applies.

## std::unique_ptr: one owner

```cpp
#include <memory>

auto widget = std::make_unique<Widget>(42);     // allocates and owns
widget->draw();
Widget& ref = *widget;

std::unique_ptr<Widget> other = std::move(widget);   // ownership moves; widget is now null
// std::unique_ptr<Widget> copy = other;             // does not compile: no copies
```

- Zero overhead compared with a raw pointer (same size, no reference count).
- Deletes the object when it is destroyed or reset.
- Move-only, so the type system enforces "exactly one owner".
- `std::make_unique<T[]>(n)` for arrays (usually prefer `std::vector`).
- A custom deleter handles non-memory resources:
  `std::unique_ptr<std::FILE, decltype(&std::fclose)> f(std::fopen(p, "r"), &std::fclose);`

Factory functions return `std::unique_ptr` to say "you own this now":

```cpp
std::unique_ptr<Shape> make_shape(std::string_view kind);
```

## std::shared_ptr and std::weak_ptr: shared ownership

```cpp
auto config = std::make_shared<Config>();     // one allocation for object + control block
auto copy = config;                           // reference count 2
config.reset();                               // count 1; the Config is still alive
```

`shared_ptr` keeps an atomic reference count and deletes the object when the last owner goes.
It costs more (two pointers wide, atomic increments on copy) and makes lifetimes harder to
reason about, so use it only when ownership is genuinely shared and no single owner exists.

**Cycles leak:** two objects that hold `shared_ptr`s to each other never reach zero. Break
cycles with `std::weak_ptr`, which observes without owning:

```cpp
struct Node {
    std::vector<std::shared_ptr<Node>> children;
    std::weak_ptr<Node> parent;               // does not keep the parent alive
};

if (auto p = node->parent.lock()) {            // a shared_ptr, or empty if the parent died
    p->notify();
}
```

## Raw pointers and references in modern code

Ownership is expressed with values, containers and smart pointers. Raw pointers and
references are **non-owning**: they observe an object someone else keeps alive.

```cpp
void render(const Widget& w);          // must exist, not owned
void render(const Widget* w);          // may be null, not owned
void adopt(std::unique_ptr<Widget> w); // takes ownership
void share(std::shared_ptr<Widget> w); // takes a share of ownership
```

The Core Guidelines summarise it: "never transfer ownership by a raw pointer" (R.3) and
"avoid calling `new` and `delete` explicitly" (R.11). If you see `new` in application code,
ask which RAII type should be doing it.

## Destructors and recursion

A `unique_ptr` chain (`Node` owning `next`) is freed recursively by default: one stack
frame per node. For long chains write an iterative destructor:

```cpp
~List() {
    while (head_) {
        head_ = std::move(head_->next);   // the old head dies with no next left to free
    }
}
```

## Summary

- Tie each resource to an object: acquire in the constructor, release in the destructor.
- Destructors run at scope exit, also during exceptions, so RAII cannot leak.
- `std::unique_ptr` (via `make_unique`) for single ownership; it is free.
- `std::shared_ptr` only for real shared ownership; `std::weak_ptr` breaks cycles.
- Raw pointers and references observe; they never own.

## Further reading

- cppreference, RAII: <https://en.cppreference.com/w/cpp/language/raii>
- cppreference, std::unique_ptr: <https://en.cppreference.com/w/cpp/memory/unique_ptr>
- cppreference, std::shared_ptr: <https://en.cppreference.com/w/cpp/memory/shared_ptr>
- C++ Core Guidelines, R: Resource management: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-resource>
- Herb Sutter, "GotW #91 Solution: Smart Pointer Parameters": <https://herbsutter.com/2013/06/05/gotw-91-solution-smart-pointer-parameters/>
- Scott Meyers, *Effective Modern C++*, items 18–22
