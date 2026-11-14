# Ownership: Memory Without a Garbage Collector

> Ownership is the idea that makes Rust Rust. It decides when memory is freed, with no garbage
> collector and no manual `free`, and the compiler checks every rule before the program runs.
> It feels strict for a week, then it becomes how you think.

**In this lesson you will learn**

- the stack and the heap, and why it matters which one a value lives on
- the three ownership rules
- what a **move** is, and why using a moved value is a compile error
- which types are **copied** instead of moved
- `clone`, and why Rust makes you write it

---

## 1. Stack and heap in one minute

- The **stack** holds values whose size is known at compile time: numbers, booleans, fixed
  arrays. Pushing and popping is extremely fast, and everything is freed when the function returns.
- The **heap** holds data whose size can change at runtime, like the text of a `String` or the
  items of a `Vec`. The program asks for memory, gets a pointer, and must give it back later.

A `String` is both: a small part on the stack (pointer, length, capacity) that points to the
characters on the heap.

```
stack                     heap
name ┌──────────┐        ┌───┬───┬───┬───┬───┬───┐
     │ ptr ─────┼──────▶ │ F │ e │ r │ r │ i │ s │
     │ len: 6   │        └───┴───┴───┴───┴───┴───┘
     │ cap: 6   │
     └──────────┘
```

The hard question every language answers differently: **who frees the heap part, and when?**

| Language | Answer |
| --- | --- |
| C | you call `free`, and bugs happen when you forget or do it twice |
| Java, Go, Python | a garbage collector finds unreachable objects and frees them later |
| Rust | the **owner** frees it, at a point the compiler knows exactly |

## 2. The three rules

1. Every value has exactly **one owner**: the variable that holds it.
2. When the owner goes **out of scope**, the value is **dropped** (its memory is freed).
3. Ownership can be **moved** to another variable. After a move, the old variable can't be used.

```rust
fn main() {
    {
        let name = String::from("Ferris");   // name owns the String
        println!("{name}");
    }                                        // scope ends: the String is dropped here
    // name does not exist here
}
```

Dropping is deterministic: you can point at the closing brace where it happens. This is the
same idea as RAII in C++.

## 3. Move

```rust
let a = String::from("dojo");
let b = a;              // ownership moves from a to b
println!("{a}");
```

```
error[E0382]: borrow of moved value: `a`
 --> src/main.rs:3:15
  |
1 |     let a = String::from("dojo");
  |         - move occurs because `a` has type `String`, which does not implement the `Copy` trait
2 |     let b = a;
  |             - value moved here
3 |     println!("{a}");
  |               ^^^ value borrowed here after move
```

Why not just copy the pointer and let both use it? Because then two variables would own the
same heap memory, and at the end of the scope both would free it: a **double free**, one of the
classic crashes in C. Rust avoids it by making `a` unusable after the move. Only `b` will free
the memory.

The same happens when you pass a value to a function:

```rust
fn shout(text: String) {
    println!("{}!", text.to_uppercase());
}                                   // text is dropped here

fn main() {
    let word = String::from("hey");
    shout(word);                    // word moves into the function
    // shout(word);                 // error[E0382]: use of moved value: `word`
}
```

And when a function returns a value, ownership moves out to the caller:

```rust
fn make_greeting() -> String {
    String::from("hello")           // ownership goes to whoever called us
}
```

## 4. Copy: small values are duplicated

Integers, floats, `bool`, `char`, and tuples or arrays made only of them live entirely on the
stack. Copying them is as cheap as moving, so Rust copies:

```rust
let x = 5;
let y = x;            // x is copied
println!("{x} {y}");  // 5 5: both still usable
```

These types implement the `Copy` trait. A type that owns heap memory (`String`, `Vec`, `Box`)
can never be `Copy`.

## 5. Clone: an explicit deep copy

When you really need two independent values, ask for it:

```rust
let a = String::from("dojo");
let b = a.clone();          // copies the characters to new heap memory
println!("{a} {b}");        // both are usable
```

`clone()` costs time and memory, and Rust makes it visible on purpose. When you see many
`.clone()` calls added just to make the compiler happy, it is usually a sign that the code
should **borrow** instead, which is the next lesson.

> [!NOTE]
> **Coming from Java:** `String b = a;` copies a reference, and both variables share one object
> until the garbage collector frees it. In Rust `let b = a;` moves the value and `a` becomes
> unusable. Sharing is still possible, but you ask for it explicitly with references (`&a`) or,
> later, with smart pointers like `Rc`.

## 6. Reading the move rules in practice

| Code | What happens to `s` (a `String`) |
| --- | --- |
| `let t = s;` | moved into `t` |
| `takes(s);` | moved into the function parameter |
| `let t = s.clone();` | copied, `s` still usable |
| `takes(&s);` | borrowed, `s` still usable (next lesson) |
| `let n = s.len();` | only read through a method, `s` still usable |

> [!TIP]
> When the compiler says "value moved here", look at the line it points to and ask: does that
> code need to **own** the value, or only to **look** at it? If it only looks, pass a reference.

## Practice

1. Create `let a = String::from("rust");`, move it into `b`, and print `a`. Read the error.
   Fix it in two ways: with `clone()`, and by printing `b` instead.
2. Write `fn length(text: String) -> usize` and call it twice with the same variable. Why does
   the second call fail? (The next lesson shows the proper fix.)
3. Do the same with an `i32` variable passed to `fn double(n: i32) -> i32`. Why does it work?

## Cheat sheet

| Rule | In one line |
| --- | --- |
| One owner | each value belongs to exactly one variable |
| Drop | the value is freed when its owner goes out of scope |
| Move | assigning or passing a heap value transfers ownership |
| Copy | numbers, `bool`, `char` are copied instead |
| Clone | `.clone()` makes an explicit, independent copy |

**Ready?** Press **Q** in the dojo for the quiz.
