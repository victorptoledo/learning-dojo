# Smart Pointers: Box, Rc, RefCell and Arc

> Plain references borrow; owners own. Smart pointers are owners with an extra ability: putting
> data on the heap, sharing it between several owners, or allowing changes the borrow checker
> can't prove safe at compile time. Each one solves one problem, and the names tell you which.

**In this lesson you will learn**

- `Box<T>`: heap allocation, recursive types and trait objects
- `Rc<T>`: several owners on one thread
- `RefCell<T>`: mutation through a shared reference, checked at runtime
- `Arc<T>` and `Mutex<T>`: the thread-safe versions
- how to pick the right one, and the trap of reference cycles

---

## 1. `Box<T>`: put a value on the heap

```rust
let big = Box::new([0u8; 1_000_000]);   // the array lives on the heap
println!("{}", big.len());              // use it like the value itself
```

A `Box` owns its value: when the box is dropped, so is the value. Moving a `Box` only moves the
pointer, not the million bytes. Three reasons to use one:

**Recursive types.** A type can't contain itself directly: its size would be infinite.

```rust
enum List {
    Node(i32, Box<List>),
    Empty,
}

use List::{Empty, Node};
let list = Node(1, Box::new(Node(2, Box::new(Empty))));
```

```
error[E0072]: recursive type `List` has infinite size   // without the Box
```

A `Box` has a fixed size (one pointer), so the compiler can lay the enum out.

**Trait objects.** `Box<dyn Trait>` holds "some type implementing Trait" (previous lesson).

**Large values** that you want to move around cheaply.

## 2. `Rc<T>`: several owners, one thread

Sometimes one value genuinely has several owners: a configuration shared by many parts of a
program, or a node with several parents in a graph. `Rc` (reference counted) keeps a count of
owners and frees the value when the last one goes away.

```rust
use std::rc::Rc;

let config = Rc::new(String::from("dark mode"));
let for_editor = Rc::clone(&config);    // count: 2, no data copied
let for_terminal = Rc::clone(&config);  // count: 3
println!("{}", Rc::strong_count(&config));   // 3

drop(for_editor);
println!("{}", Rc::strong_count(&config));   // 2
```

- `Rc::clone` only increments a counter. Writing `Rc::clone(&x)` rather than `x.clone()` makes
  that obvious to the reader.
- An `Rc` gives **shared, read-only** access: you can't get a `&mut` through it.
- `Rc` is **not thread-safe** (its counter isn't atomic). The compiler won't let you send it to
  another thread.

## 3. `RefCell<T>`: borrow rules checked at runtime

The borrowing rules are checked at compile time, and occasionally the compiler can't prove a
correct program is safe. `RefCell` moves the check to runtime: you can change the inner value
through a shared reference ("interior mutability").

```rust
use std::cell::RefCell;

let log = RefCell::new(Vec::new());

log.borrow_mut().push("started");      // a temporary &mut, released at the end of the line
log.borrow_mut().push("loaded");
println!("{:?}", log.borrow());        // ["started", "loaded"]
```

The same rule applies (many readers or one writer), but breaking it **panics** instead of failing
to compile:

```rust
let first = log.borrow_mut();
let second = log.borrow_mut();   // panics: already mutably borrowed: BorrowError
```

`Rc<RefCell<T>>` is the classic combination for data that is **shared and mutable** on one thread:

```rust
use std::cell::RefCell;
use std::rc::Rc;

let scores = Rc::new(RefCell::new(vec![10, 20]));
let shared = Rc::clone(&scores);
shared.borrow_mut().push(30);
println!("{:?}", scores.borrow());     // [10, 20, 30]
```

> [!WARNING]
> **Reference cycles leak.** If two `Rc` values point at each other, their counts never reach
> zero and neither is freed. For back-pointers (a child pointing at its parent), use `Weak<T>`
> (from `Rc::downgrade`), which doesn't keep the value alive.

## 4. `Arc<T>` and `Mutex<T>`: across threads

`Arc` (atomically reference counted) is `Rc` with a thread-safe counter. `Mutex` guards a value
so only one thread at a time can change it:

```rust
use std::sync::{Arc, Mutex};
use std::thread;

let counter = Arc::new(Mutex::new(0));
let mut handles = Vec::new();

for _ in 0..4 {
    let counter = Arc::clone(&counter);
    handles.push(thread::spawn(move || {
        *counter.lock().unwrap() += 1;   // lock, change, unlock at the end of the statement
    }));
}
for handle in handles {
    handle.join().unwrap();
}
println!("{}", *counter.lock().unwrap());   // 4
```

`Arc<Mutex<T>>` is to threads what `Rc<RefCell<T>>` is to a single thread. The next lesson
covers threads in detail.

## 5. Choosing

| I need... | Use | Checked |
| --- | --- | --- |
| a value on the heap, a recursive type, `dyn Trait` | `Box<T>` | compile time |
| several owners, one thread, read-only | `Rc<T>` | compile time |
| to change something behind `&`, one thread | `RefCell<T>` | runtime (panics) |
| shared and mutable, one thread | `Rc<RefCell<T>>` | runtime |
| several owners across threads | `Arc<T>` | compile time |
| shared and mutable across threads | `Arc<Mutex<T>>` | runtime (blocks) |

> [!NOTE]
> **Coming from Java:** in Java every object is on the heap and shared freely, and the garbage
> collector sorts out lifetimes. In Rust you choose: most values are owned plainly, and you reach
> for `Rc`/`Arc` only where sharing is real. The type then documents that sharing.

> [!TIP]
> Start with plain ownership and references. Add a smart pointer when the compiler shows you a
> real need for it, not up front.

## Practice

1. Build the `List` enum above with three nodes and write `fn sum(list: &List) -> i32` with
   `match` and recursion.
2. Share an `Rc<String>` between three variables, printing `Rc::strong_count` after each clone
   and after each `drop`.
3. Make two `borrow_mut()` calls on the same `RefCell` alive at once and read the panic.
4. Change the `Arc<Mutex<i32>>` example to 10 threads that each add 100.

## Cheat sheet

| Smart pointer | One-line summary |
| --- | --- |
| `Box<T>` | owns a value on the heap |
| `Rc<T>` | shared ownership, counted, single thread |
| `Weak<T>` | non-owning pointer that breaks `Rc` cycles |
| `RefCell<T>` | borrow rules at runtime, single thread |
| `Arc<T>` | `Rc` for threads |
| `Mutex<T>` | one thread at a time may access the value |

**Ready?** Press **Q** in the dojo for the quiz.
