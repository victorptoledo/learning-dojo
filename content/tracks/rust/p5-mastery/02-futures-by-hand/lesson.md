# Futures by Hand: poll, Waker and Pin

> Tokio hides a simple machine. A future is a value with one method, `poll`. A runtime calls it,
> gets `Pending`, and waits until someone calls the **waker** it handed over. Knowing that
> machine explains every async rule you met in the last lesson: why futures are lazy, why
> blocking freezes a runtime, why dropping a future cancels it, and what `Pin` is about.

**In this lesson you will learn**

- the `Future` trait, `Poll` and `Context`
- the waker contract: who calls `wake`, and when
- writing a future by hand: a timer that does not block
- writing a tiny executor, `block_on`, with only the standard library
- combinators: joining and timing out futures
- why `async` blocks need `Pin`, and how `Box::pin` handles it

---

## 1. The `Future` trait

```rust
pub trait Future {
    type Output;
    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output>;
}

pub enum Poll<T> {
    Ready(T),
    Pending,
}
```

`poll` must **return quickly**. It either finishes (`Ready`) or says "not yet" (`Pending`). An
`async fn` compiles into a state machine whose `poll` runs the code up to the next `.await`
that is not ready, then returns `Pending`.

## 2. The waker contract

`Pending` alone would leave the runtime guessing when to poll again. So the rule is:

> A future that returns `Pending` must make sure `cx.waker()` is woken when it can make progress.

```rust
use std::future::Future;
use std::pin::Pin;
use std::task::{Context, Poll};

struct Ready(u32);

impl Future for Ready {
    type Output = u32;
    fn poll(self: Pin<&mut Self>, _cx: &mut Context<'_>) -> Poll<u32> {
        Poll::Ready(self.0)                        // never pending: no waker needed
    }
}
```

The runtime may poll with a **different** waker each time (a task can move between threads),
so a future that stores a waker must replace it with the newest one on every poll:
`waker.clone_from(cx.waker())`.

## 3. A timer that does not block

`std::thread::sleep` inside `poll` would block the whole executor. Instead, hand the waiting to
another thread and wake the task when time is up:

```rust
use std::sync::{Arc, Mutex};
use std::task::Waker;
use std::time::{Duration, Instant};

pub struct Sleep {
    deadline: Instant,
    waker: Option<Arc<Mutex<Option<Waker>>>>,   // shared with the timer thread
}

pub fn sleep(duration: Duration) -> Sleep {
    Sleep { deadline: Instant::now() + duration, waker: None }
}

impl Future for Sleep {
    type Output = ();
    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<()> {
        if Instant::now() >= self.deadline {
            return Poll::Ready(());
        }
        match &self.waker {
            Some(slot) => *slot.lock().unwrap() = Some(cx.waker().clone()),   // newest waker
            None => {
                let slot = Arc::new(Mutex::new(Some(cx.waker().clone())));
                let timer = Arc::clone(&slot);
                let deadline = self.deadline;
                std::thread::spawn(move || {
                    std::thread::sleep(deadline.saturating_duration_since(Instant::now()));
                    if let Some(waker) = timer.lock().unwrap().take() {
                        waker.wake();
                    }
                });
                self.waker = Some(slot);
            }
        }
        Poll::Pending
    }
}
```

Real runtimes use one timer wheel instead of a thread per sleep, but the contract is the same.

## 4. An executor in twenty lines

`block_on` polls a future on the current thread. Its waker **unparks** that thread; between
polls the thread **parks**, so it uses no CPU while it waits.

```rust
use std::task::Wake;
use std::thread::{self, Thread};

struct ThreadWaker(Thread);

impl Wake for ThreadWaker {
    fn wake(self: Arc<Self>) {
        self.0.unpark();
    }
}

pub fn block_on<F: Future>(future: F) -> F::Output {
    let mut future = std::pin::pin!(future);              // pin it on the stack
    let waker = Waker::from(Arc::new(ThreadWaker(thread::current())));
    let mut cx = Context::from_waker(&waker);
    loop {
        match future.as_mut().poll(&mut cx) {
            Poll::Ready(value) => return value,
            Poll::Pending => thread::park(),               // sleep until woken
        }
    }
}

let three = block_on(async {
    sleep(Duration::from_millis(100)).await;
    3
});
```

`thread::park` may return without a wake ("spurious wake-ups"); the loop simply polls again.
A loop that polls **without** parking also works, and burns a whole core doing it.

## 5. Combinators: join and timeout

Concurrency on one thread is just polling several futures from one `poll`. A join keeps the
futures still running and their results:

```rust
pub struct Join2<A: Future, B: Future> {
    a: Pin<Box<A>>, b: Pin<Box<B>>,
    a_out: Option<A::Output>, b_out: Option<B::Output>,
}

impl<A: Future, B: Future> Future for Join2<A, B>
where A::Output: Unpin, B::Output: Unpin
{
    type Output = (A::Output, B::Output);
    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        let this = &mut *self;                            // fine: every field is Unpin
        if this.a_out.is_none() {
            if let Poll::Ready(v) = this.a.as_mut().poll(cx) { this.a_out = Some(v); }
        }
        if this.b_out.is_none() {
            if let Poll::Ready(v) = this.b.as_mut().poll(cx) { this.b_out = Some(v); }
        }
        if this.a_out.is_some() && this.b_out.is_some() {
            return Poll::Ready((this.a_out.take().unwrap(), this.b_out.take().unwrap()));
        }
        Poll::Pending
    }
}
```

Note the `is_none()` checks: a future must **not** be polled again after it returned `Ready`
(many panic if you do).

A **timeout** polls the work first, then a `Sleep`; if the sleep wins, it returns an error and
the work future is **dropped**. That drop is the cancellation you met with Tokio: the state
machine is destroyed mid-way, and its `Drop` impls release what it held.

## 6. Why `Pin`

An `async` block can hold a reference to its own local variable across an `.await`:

```rust
async {
    let data = [1, 2, 3];
    let first = &data[0];          // points into the future's own state
    sleep(Duration::from_millis(10)).await;
    println!("{first}");
}
```

If that future moved in memory after the first poll, `first` would dangle. `Pin<&mut Self>`
promises it will **not move again**. Types that do not care (almost all normal types) are
`Unpin`, and for them `Pin` changes nothing.

| You have | To poll it |
| --- | --- |
| a future you own, on the stack | `let f = std::pin::pin!(f);` then `f.as_mut().poll(cx)` |
| a future you store in a struct | `Pin<Box<F>>` via `Box::pin(f)`: the heap address never moves |
| an `Unpin` future | `Pin::new(&mut f)` |
| many different futures | `Vec<Pin<Box<dyn Future<Output = T> + Send>>>` |

`Box::pin` costs one allocation and removes nearly every `Pin` headache, which is why it is the
usual choice outside hot paths.

## Practice

1. Run `block_on(sleep(Duration::from_secs(1)))` and watch the CPU: it stays near zero. Replace
   `thread::park()` with nothing and watch again.
2. Count how many times `poll` is called on a `Sleep` of 200 ms under your `block_on`.
3. Write `Join2` for two `Sleep`s of 300 ms and time it.
4. Write `timeout(duration, future)` returning `Result<T, Elapsed>`.

## Cheat sheet

| I want to... | Write |
| --- | --- |
| a future by hand | `impl Future for T { type Output = ...; fn poll(self: Pin<&mut Self>, cx) ... }` |
| say "not yet" | store `cx.waker().clone()`, arrange a `wake()`, return `Poll::Pending` |
| a waker from my own type | `impl Wake for W`, then `Waker::from(Arc::new(w))` |
| wait without burning CPU | `thread::park()` in the executor, `thread.unpark()` in `wake` |
| pin on the stack | `std::pin::pin!(future)` |
| store any future | `Pin<Box<dyn Future<Output = T> + Send>>`, made with `Box::pin` |
| cancel a future | drop it |

**Ready?** Press **Q** in the dojo for the quiz.
