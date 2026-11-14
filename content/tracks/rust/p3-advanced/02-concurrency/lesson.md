# Threads, Channels (mpsc) and Mutex

> In most languages concurrency bugs (data races, two threads writing the same memory) show up
> as rare, unreproducible crashes. In Rust they are compile errors. The ownership and borrowing
> rules you already know are exactly what makes that possible: Rust calls it fearless concurrency.

**In this lesson you will learn**

- how to start threads and wait for them with `join`
- why thread closures need `move`
- message passing with channels (`mpsc`)
- shared state with `Arc<Mutex<T>>`, and how locks are released
- `Send` and `Sync`, the two traits the compiler uses to keep threads safe
- scoped threads, for borrowing local data safely

---

## 1. Starting a thread

```rust
use std::thread;
use std::time::Duration;

fn main() {
    let handle = thread::spawn(|| {
        for i in 1..=3 {
            println!("worker: {i}");
            thread::sleep(Duration::from_millis(10));
        }
        42                                   // the thread's result
    });

    println!("main keeps going");
    let answer = handle.join().unwrap();     // wait for the thread and take its result
    println!("worker returned {answer}");
}
```

- `thread::spawn` runs the closure on a new operating-system thread and returns a `JoinHandle`.
- `join()` blocks until the thread ends. It returns `Err` if the thread panicked.
- Without `join`, `main` may finish first, and the program exits with the worker still running.

The order of "main keeps going" and "worker: 1" changes from run to run: that's concurrency.

## 2. `move`: give the thread its own data

```rust
let name = String::from("Ferris");
let handle = thread::spawn(|| {
    println!("hello {name}");
});
```

```
error[E0373]: closure may outlive the current function, but it borrows `name`, which is owned by the current function
help: to force the closure to take ownership of `name`, use the `move` keyword
```

The new thread might still be running after `main`'s variables are gone, so it can't borrow
them. `move` transfers ownership into the closure:

```rust
let name = String::from("Ferris");
let handle = thread::spawn(move || {
    println!("hello {name}");                // the thread owns name now
});
handle.join().unwrap();
```

## 3. Message passing with channels

"Don't communicate by sharing memory; share memory by communicating." A channel has a sending
end and a receiving end:

```rust
use std::sync::mpsc;
use std::thread;

fn main() {
    let (sender, receiver) = mpsc::channel();

    for id in 0..3 {
        let sender = sender.clone();             // one sender per thread
        thread::spawn(move || {
            sender.send(id * 10).unwrap();       // send moves the value into the channel
        });
    }
    drop(sender);                                // drop the original, or the loop never ends

    let total: i32 = receiver.iter().sum();     // receives until every sender is gone
    println!("{total}");                         // 30
}
```

- **mpsc** = multiple producers, single consumer: clone the sender freely, there is one receiver.
- `send` moves the value: once sent, the sending thread can't touch it. No sharing, no race.
- `receiver.recv()` waits for one message; `receiver.iter()` loops until all senders are dropped.

> [!WARNING]
> **Common mistake:** forgetting `drop(sender)`. The original sender is still alive in `main`,
> so the receiving loop waits forever and the program hangs.

## 4. Shared state with `Arc<Mutex<T>>`

When several threads must update the same value:

```rust
use std::sync::{Arc, Mutex};
use std::thread;

fn main() {
    let counter = Arc::new(Mutex::new(0));
    let mut handles = Vec::new();

    for _ in 0..8 {
        let counter = Arc::clone(&counter);
        handles.push(thread::spawn(move || {
            let mut value = counter.lock().unwrap();   // waits for the lock
            *value += 1;
        }));                                           // value dropped: lock released
    }
    for handle in handles {
        handle.join().unwrap();
    }
    println!("{}", *counter.lock().unwrap());           // 8
}
```

- `Arc` lets every thread own a handle to the same `Mutex`.
- `lock()` returns a **guard**. While it lives, no other thread can lock; when it is dropped,
  the lock is released. You can't forget to unlock.
- `lock()` returns `Err` only if another thread panicked while holding the lock ("poisoned").

> [!TIP]
> Keep the guard's life short: lock, change, let it go. Holding a lock while doing slow work
> (I/O, sleeping) makes every other thread wait.

## 5. `Send` and `Sync`: how the compiler knows

| Trait | Means | Examples that are **not** |
| --- | --- | --- |
| `Send` | the value can be **moved** to another thread | `Rc<T>` |
| `Sync` | the value can be **shared** (`&T`) between threads | `RefCell<T>`, `Rc<T>` |

They are implemented automatically when every part of a type is `Send`/`Sync`. `thread::spawn`
requires its closure to be `Send`, so this fails at compile time:

```rust
use std::rc::Rc;
let shared = Rc::new(5);
thread::spawn(move || println!("{shared}"));
// error[E0277]: `Rc<i32>` cannot be sent between threads safely
```

Replace `Rc` with `Arc` and it compiles. The compiler has turned a runtime race into a type error.

## 6. Scoped threads: borrowing local data

When the threads must finish before the function continues, `thread::scope` lets them borrow
local variables, with no `move` and no `Arc`:

```rust
let numbers = vec![1, 2, 3, 4, 5, 6];
let (left, right) = numbers.split_at(3);

let (a, b) = thread::scope(|scope| {
    let first = scope.spawn(|| left.iter().sum::<i32>());
    let second = scope.spawn(|| right.iter().sum::<i32>());
    (first.join().unwrap(), second.join().unwrap())
});
println!("{}", a + b);   // 21
```

All threads started inside the scope are joined before `scope` returns, so the borrows are safe.

> [!NOTE]
> **Coming from Java:** `thread::spawn` is `new Thread(...).start()`, `join` is `join()`,
> channels play the role of `BlockingQueue`, and `Mutex<T>` is like `synchronized` except the
> lock **owns** the data: you can't reach the value without holding the lock. And sharing a
> non-thread-safe object across threads is a compile error instead of a production incident.

## Practice

1. Start 5 threads that each print their number, and join them all in order.
2. Use a channel to send the squares of 1 to 10 from a worker thread, and add them up in `main`.
3. Remove `drop(sender)` from the channel example and watch it hang. Put it back.
4. Split a `Vec` of 1 000 numbers in four with `chunks(250)` and sum the parts in scoped threads.

## Cheat sheet

| I want to... | Use |
| --- | --- |
| run something in parallel | `thread::spawn(move \|\| ...)` |
| wait for it and get its result | `handle.join().unwrap()` |
| send values between threads | `mpsc::channel()` |
| share a value that changes | `Arc<Mutex<T>>` |
| borrow local data in threads | `thread::scope(\|s\| ...)` |
| know why it doesn't compile | look for `Send` / `Sync` in the error |

**Ready?** Press **Q** in the dojo for the quiz.
