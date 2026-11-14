# async/await and the Tokio Runtime

> Threads are great for CPU-heavy work, but a server waiting on ten thousand network connections
> would need ten thousand threads, each with its own stack. Async code lets a handful of threads
> juggle thousands of tasks that spend most of their time waiting.

**In this lesson you will learn**

- what a future is, and why Rust futures do nothing until awaited
- `async fn` and `.await`
- why you need a runtime, and how Tokio provides one
- running tasks concurrently: `tokio::spawn`, `join!`, `select!`
- timeouts and cancellation
- the one rule never to break: don't block inside async code

---

## 1. Futures are lazy

An `async fn` doesn't run when you call it. It returns a **future**: a value that describes work
still to be done.

```rust
async fn fetch_name() -> String {
    String::from("Ferris")
}

fn main() {
    let future = fetch_name();   // nothing has run yet
    // without .await or a runtime, the body is never executed
}
```

The compiler even warns you: `unused implementor of Future that must be used`.

## 2. `.await`

Inside another `async` function, `.await` runs a future and gives you its result:

```rust
async fn greet() {
    let name = fetch_name().await;
    println!("hello {name}");
}
```

While a future waits for something (a socket, a timer, a file), `.await` **yields**: the runtime
parks this task and runs other ready tasks on the same thread. When the data arrives, the task
continues exactly where it stopped.

## 3. A runtime: Tokio

The standard library defines the `Future` trait but doesn't run futures. A **runtime** does:
it keeps a queue of tasks, polls the ready ones, and talks to the operating system about I/O.
Tokio is the most used one.

```toml
[dependencies]
tokio = { version = "1", features = ["full"] }
```

```rust
#[tokio::main]
async fn main() {
    greet().await;
}
```

`#[tokio::main]` turns your `async fn main` into a normal `main` that starts the runtime and
runs your future to completion.

## 4. Running things concurrently

`.await` one after the other runs **sequentially**:

```rust
use tokio::time::{sleep, Duration};

async fn slow(label: &str, ms: u64) -> String {
    sleep(Duration::from_millis(ms)).await;   // async sleep: yields, doesn't block
    format!("{label} done")
}

#[tokio::main]
async fn main() {
    let a = slow("a", 300).await;
    let b = slow("b", 300).await;
    println!("{a}, {b}");                      // about 600 ms
}
```

`tokio::join!` runs them **concurrently** and waits for all:

```rust
let (a, b) = tokio::join!(slow("a", 300), slow("b", 300));   // about 300 ms
```

`tokio::spawn` starts an independent **task**, like a lightweight thread:

```rust
let handle = tokio::spawn(async { slow("background", 100).await });
// ... do other work ...
let result = handle.await.unwrap();   // a JoinHandle, like with threads
```

Spawned tasks must own their data (`async move { ... }`) and be `Send`, for the same reasons
as threads.

## 5. `select!`, timeouts and cancellation

`tokio::select!` waits for the **first** future to finish and drops the others:

```rust
tokio::select! {
    result = slow("download", 500) => println!("{result}"),
    _ = sleep(Duration::from_millis(200)) => println!("gave up waiting"),
}
```

**Dropping a future cancels it**: whatever it was waiting for is abandoned. That is how timeouts
work:

```rust
use tokio::time::timeout;

match timeout(Duration::from_millis(200), slow("query", 500)).await {
    Ok(result) => println!("{result}"),
    Err(_) => println!("timed out"),
}
```

## 6. Never block inside async code

A runtime runs many tasks on few threads. If one task **blocks** the thread, every task on that
thread stops:

| Blocking (don't, inside async) | Async version |
| --- | --- |
| `std::thread::sleep` | `tokio::time::sleep(...).await` |
| `std::fs::read_to_string` | `tokio::fs::read_to_string(...).await` |
| a long CPU loop | `tokio::task::spawn_blocking(...)` |
| `std::sync::Mutex` held across `.await` | `tokio::sync::Mutex` |

```rust
let digest = tokio::task::spawn_blocking(|| expensive_hash(&data))
    .await
    .unwrap();   // runs on a thread pool made for blocking work
```

> [!WARNING]
> **Common mistake:** calling `std::thread::sleep` in a task "just for a test". The whole
> runtime thread stops; other tasks miss deadlines and the program feels frozen.

## 7. When to use async

| Workload | Better fit |
| --- | --- |
| many connections, mostly waiting (web servers, proxies, clients) | async |
| heavy computation (image processing, compression) | threads, `rayon` |
| a small script or CLI | plain synchronous code |

> [!NOTE]
> **Coming from Java:** a future is like a `CompletableFuture` that doesn't start until awaited,
> and `.await` reads like synchronous code instead of a chain of `thenApply`. Java 21 virtual
> threads attack the same problem from the other side (cheap blocking threads); Rust chose async
> tasks with zero hidden allocations.

## Practice

1. Create a project with `cargo new async-play`, add Tokio with `cargo add tokio --features full`,
   and print "hello" from `#[tokio::main] async fn main`.
2. Measure the sequential and the `join!` version of the `slow` example with
   `std::time::Instant::now()` and `elapsed()`.
3. Use `timeout` to give up on a 2-second `slow` call after 500 ms.
4. Replace `tokio::time::sleep` with `std::thread::sleep` inside one of two joined tasks and
   explain the new total time.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| an async function | `async fn name() -> T` |
| run a future and get its value | `future.await` |
| start the runtime | `#[tokio::main] async fn main()` |
| run several and wait for all | `tokio::join!(a, b)` |
| run in the background | `tokio::spawn(async move { ... })` |
| take whichever finishes first | `tokio::select! { ... }` |
| give up after a while | `tokio::time::timeout(duration, future)` |
| do blocking work | `tokio::task::spawn_blocking(\|\| ...)` |

**Ready?** Press **Q** in the dojo for the quiz.
