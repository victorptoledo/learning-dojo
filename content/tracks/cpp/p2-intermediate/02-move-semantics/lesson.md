# Move Semantics and Rvalue References

Copying a `std::vector` of a million strings copies a million strings. **Moving** it copies
three pointers and leaves the source empty. C++11 added move semantics so that values can be
passed and returned freely without paying for copies that nobody needed: an object that is
about to die can hand its resources over instead of having them duplicated.

## Value categories, in practice

Every expression is either an **lvalue** (it has a name and an address you can take: a
variable) or an **rvalue** (a temporary about to disappear: `make_name()`, `a + b`,
`std::string("x")`). An rvalue can safely be robbed, because nobody can look at it afterwards.

```cpp
std::string a = "ada";
std::string b = a;                 // a is an lvalue: copy
std::string c = a + " lovelace";   // a temporary: move (or elided)
std::string d = std::move(a);      // std::move makes a look like an rvalue: move
```

`std::move` moves nothing. It is a cast to `T&&` that says "I am done with this; you may take
its resources". The move constructor of the target does the work.

## Rvalue references and the move members

`T&&` (with a concrete `T`) is an **rvalue reference**: it binds only to rvalues. A class
opts into moves with a move constructor and a move assignment:

```cpp
class Buffer {
public:
    Buffer(Buffer&& other) noexcept
        : data_(std::exchange(other.data_, nullptr)), size_(std::exchange(other.size_, 0)) {}

    Buffer& operator=(Buffer&& other) noexcept {
        if (this != &other) {
            delete[] data_;
            data_ = std::exchange(other.data_, nullptr);
            size_ = std::exchange(other.size_, 0);
        }
        return *this;
    }
    // ... constructor, destructor, copy members (the rule of five)
private:
    int* data_ = nullptr;
    std::size_t size_ = 0;
};
```

Two rules make moves work:

1. **Leave the source valid.** A moved-from object must still be destructible and
   assignable; standard types leave it "valid but unspecified" (in practice, usually
   empty). Do not read a moved-from object's value; assign to it or let it die.
2. **Mark moves `noexcept`.** `std::vector` moves its elements when it grows only if the move
   constructor cannot throw; otherwise it copies them to keep the strong exception
   guarantee. A forgotten `noexcept` silently turns every reallocation into copies.

## The rule of five and the rule of zero

If a class manages a resource by hand, it needs all five: destructor, copy constructor, copy
assignment, move constructor, move assignment. If you write any of them, think about all of
them: declaring a destructor or a copy operation suppresses the implicit moves (they fall back
to copies). Most classes should manage nothing by hand and follow the rule of zero; their
generated moves move each member.

**Copy and swap** gives a copy assignment with the strong exception guarantee and correct
self-assignment for free:

```cpp
Buffer& operator=(const Buffer& other) {
    Buffer copy(other);   // may throw: *this is untouched
    swap(copy);           // noexcept
    return *this;         // copy dies with the old contents
}
```

## Where moves happen without std::move

- **Returning a local** by value: `return result;` elides the copy (the object is built in
  the caller), or moves it. Do **not** write `return std::move(result);`: it prevents elision.
- **Temporaries**: `v.push_back(make_name());` moves the temporary in.
- **Sink parameters**: take by value and move into the member.

```cpp
class Person {
public:
    explicit Person(std::string name) : name_(std::move(name)) {}
private:
    std::string name_;
};
Person p(read_name());   // moved twice, copied never
```

`emplace_back(args...)` builds the element directly in the vector, which avoids even the
move.

## Move-only types

Some types represent unique ownership and can be moved but not copied: `std::unique_ptr`,
`std::thread`, `std::jthread`, `std::fstream`, `std::future`. Containers of them work, as long
as you move elements in and out:

```cpp
std::vector<std::unique_ptr<Shape>> shapes;
shapes.push_back(std::make_unique<Circle>(2.0));
auto first = std::move(shapes.front());
```

## Forwarding references and std::forward

In a template, `T&&` where `T` is deduced is a **forwarding reference**: it binds to
anything and remembers whether the argument was an lvalue or an rvalue. `std::forward`
passes it on with the same category:

```cpp
template <typename... Args>
std::unique_ptr<Widget> make_widget(Args&&... args) {
    return std::make_unique<Widget>(std::forward<Args>(args)...);   // perfect forwarding
}
```

Use `std::move` on rvalue references and `std::forward` on forwarding references; mixing
them up silently copies or, worse, moves from something the caller still uses.

## Summary

- Lvalues have names; rvalues are temporaries that can be robbed.
- `std::move` is a cast; the move constructor does the work.
- Moves leave the source valid and must be `noexcept`.
- Rule of five when you manage a resource, rule of zero otherwise; copy-and-swap for safe
  assignment.
- Return locals by value without `std::move`; sink parameters by value then move.
- `std::forward` for forwarding references in templates.

## Further reading

- cppreference, Move constructors: <https://en.cppreference.com/w/cpp/language/move_constructor>
- cppreference, Value categories: <https://en.cppreference.com/w/cpp/language/value_category>
- cppreference, std::move and std::forward: <https://en.cppreference.com/w/cpp/utility/move>
- cppreference, Copy elision: <https://en.cppreference.com/w/cpp/language/copy_elision>
- C++ Core Guidelines, C.66 and F.18–F.19 (moves and forwarding): <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#Rc-move-noexcept>
- Scott Meyers, *Effective Modern C++*, items 23–30
