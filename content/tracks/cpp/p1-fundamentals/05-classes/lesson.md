# Classes, Constructors, Invariants and Operators

A class groups data with the operations that keep it consistent. The C++ way to design one
is to ask: **what must always be true about this object** (its *invariant*), and which
members can break it? Constructors establish the invariant, member functions keep it, and
private data makes sure nobody else breaks it.

## struct or class

`struct` and `class` differ only in default access (`public` for struct, `private` for class).
By convention: a `struct` is a plain bundle of data with no invariant (`Point`, `Item`); a
`class` has private data and an invariant to protect.

```cpp
struct Point {
    int x = 0;     // default member initialisers
    int y = 0;
    bool operator==(const Point&) const = default;   // C++20: compares every member
};

Point p{3, 4};          // aggregate initialisation
```

## A class with an invariant

```cpp
class Account {
public:
    explicit Account(std::string owner, std::int64_t opening_cents = 0);

    void deposit(std::int64_t cents);          // changes the object
    bool withdraw(std::int64_t cents);         // false if it would go negative
    std::int64_t balance() const { return balance_; }   // const: does not change it
    const std::string& owner() const { return owner_; }

private:
    std::string owner_;
    std::int64_t balance_ = 0;    // invariant: never negative
};
```

```cpp
Account::Account(std::string owner, std::int64_t opening_cents)
    : owner_(std::move(owner)), balance_(opening_cents) {     // member initialiser list
    if (owner_.empty() || balance_ < 0) {
        throw std::invalid_argument("Account: bad owner or balance");
    }
}

bool Account::withdraw(std::int64_t cents) {
    if (cents <= 0 || cents > balance_) {
        return false;          // check first, then change: a refused call changes nothing
    }
    balance_ -= cents;
    return true;
}
```

- The **member initialiser list** (`: owner_(...)`) initialises members; assigning in the body
  would default-construct them first. Members are initialised **in declaration order**, not
  in the order of the list.
- **`explicit`** on single-argument constructors stops surprising implicit conversions
  (`Account a = "ada";`).
- **`const` member functions** can be called on `const` objects and references. Mark every
  member that does not change the object `const`; it is part of the interface.
- A constructor that cannot establish the invariant throws (the object then never exists).
  Classes whose failure is expected can use a static factory returning `std::optional` or
  `std::expected` instead.

## Special members and the rule of zero

The compiler can write six special members for you: the default constructor, the destructor,
the copy constructor and copy assignment, the move constructor and move assignment. For a
class whose members already manage themselves (`std::string`, `std::vector`, smart pointers),
the generated versions are correct. That is the **rule of zero**: write none of them.

```cpp
class Library {
    std::string name_;
    std::vector<Book> books_;
    // no destructor, no copy, no move written: all correct and efficient
};
```

You only write them when the class manages a resource by hand (the RAII lesson and the
resource-handle kata) or to forbid them:

```cpp
class Connection {
public:
    Connection(const Connection&) = delete;             // not copyable
    Connection& operator=(const Connection&) = delete;
};
```

## Operators

Overload operators when they mean what they mean for built-in types:

```cpp
struct Money {
    std::int64_t cents = 0;
    auto operator<=>(const Money&) const = default;   // ==, <, >, <=, >= in one line (C++20)
    Money& operator+=(Money other) { cents += other.cents; return *this; }
    friend Money operator+(Money a, Money b) { return a += b; }
};

std::ostream& operator<<(std::ostream& out, const Money& m);   // for printing
```

For `std::format`/`std::print`, specialise `std::formatter<Money>`. Keep symmetric operators
(`+`, `==`) as free functions (or hidden friends) so conversions work on both sides.

## static members and namespaces

```cpp
class Id {
public:
    static Id next();         // belongs to the class, not to an object
private:
    static inline int counter_ = 0;
};
```

Group related code in a **namespace** (`namespace dojo { ... }`) rather than a class with only
static members; C++ does not need a class for every function.

## Encapsulation without getters for everything

Do not write a getter and a setter for every field by reflex. If any value is valid for a
field, it is data: make it a public member of a struct. If there is an invariant, expose
operations that keep it (`withdraw`), not setters that let callers break it
(`set_balance`).

## Summary

- `struct` for plain data, `class` for an invariant behind private members.
- Constructors establish the invariant (initialiser list, `explicit`); members keep it.
- `const` member functions for everything that does not change the object.
- Rule of zero: let members manage themselves; `= delete` to forbid copies.
- `= default` comparisons and `<=>` save boilerplate.

## Further reading

- C++ Core Guidelines, C: Classes and class hierarchies (C.2, C.20, C.41, C.46): <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-class>
- cppreference, Constructors and member initializer lists: <https://en.cppreference.com/w/cpp/language/constructor>
- cppreference, The rule of three/five/zero: <https://en.cppreference.com/w/cpp/language/rule_of_three>
- cppreference, Default comparisons: <https://en.cppreference.com/w/cpp/language/default_comparisons>
- cppreference, operator overloading: <https://en.cppreference.com/w/cpp/language/operators>
- Bjarne Stroustrup, *A Tour of C++* (3rd edition), chapters 5 and 6
