# Threads, Mutexes, Condition Variables and the Memory Model

C++ gives you real operating-system threads and a precise **memory model** that says when one
thread is guaranteed to see what another wrote. The one rule to remember: two threads
touching the same memory, at least one writing, with no synchronisation, is a **data race**,
and a data race is undefined behaviour. Everything in this lesson is a tool to avoid it.

## Threads

```cpp
#include <thread>

void work(int id);

std::thread t(work, 1);
t.join();                     // wait for it; a joinable std::thread destroyed = std::terminate

std::jthread j(work, 2);      // C++20: joins in its destructor (RAII), and can be stopped
```

Prefer `std::jthread`. It takes a `std::stop_token` if the function wants one, so a long loop
can be asked to stop cooperatively:

```cpp
std::jthread poller([](std::stop_token stop) {
    while (!stop.stop_requested()) {
        poll_once();
    }
});
poller.request_stop();        // also done automatically by the destructor
```

Arguments are **copied** into the thread. Pass references with `std::ref`, and make sure the
referenced objects outlive the thread. `std::thread::hardware_concurrency()` hints how many
cores you have.

## Mutexes and locks

```cpp
#include <mutex>

class Counter {
public:
    void add(int n) {
        const std::lock_guard lock(mutex_);    // locked here, unlocked at the }
        total_ += n;
    }
    int total() const {
        const std::lock_guard lock(mutex_);    // reads need the lock too
        return total_;
    }
private:
    mutable std::mutex mutex_;                 // mutable: lockable in const members
    int total_ = 0;
};
```

- Always lock through an RAII type: `std::lock_guard`, `std::unique_lock` (can unlock early,
  needed by condition variables) or `std::scoped_lock` (locks several mutexes without
  deadlock).
- Keep the mutex next to the data it protects, and keep critical sections short; never call
  unknown code (callbacks) while holding a lock.
- **Deadlock**: two threads lock two mutexes in opposite orders. Lock in a fixed order, or
  lock both at once with `std::scoped_lock lock(a, b);`.
- `std::shared_mutex` allows many readers or one writer; measure before using it.

## Condition variables

A condition variable lets a thread sleep until another thread changes the state:

```cpp
std::mutex mutex;
std::condition_variable ready_cv;
std::deque<Job> jobs;
bool closed = false;

// consumer
std::unique_lock lock(mutex);
ready_cv.wait(lock, [&] { return closed || !jobs.empty(); });   // releases the lock while asleep

// producer
{
    const std::lock_guard lock(mutex);
    jobs.push_back(job);
}
ready_cv.notify_one();
```

Always wait **with a predicate**: wake-ups can be spurious, and a notification sent before
the waiter started waiting is lost; the predicate re-checks the real state. Change the state
under the mutex, then notify. This is exactly the structure of the task-queue kata.

## Atomics and the memory model

For a single variable, `std::atomic<T>` gives race-free reads and writes without a mutex:

```cpp
std::atomic<int> hits{0};
++hits;                        // an atomic read-modify-write
int now = hits.load();
```

Atomic operations take a **memory order**. The default, `memory_order_seq_cst`, behaves as if
all atomic operations happened in one global order, which is the easiest to reason about.
`acquire`/`release` pairs are cheaper and enough for "publish data, then set a flag":

```cpp
Data payload;
std::atomic<bool> published{false};

// writer                                 // reader
payload = build();                        while (!published.load(std::memory_order_acquire)) {}
published.store(true,                     use(payload);   // sees the writer's payload
    std::memory_order_release);
```

The release store **happens before** the acquire load that reads it, so everything the writer
did before is visible to the reader. `relaxed` gives atomicity without ordering (counters).
Beyond these patterns, lock-free code is expert territory: use a mutex until profiling says
otherwise.

## Higher-level tools

| Tool | For |
| --- | --- |
| `std::async(std::launch::async, f)` → `std::future<R>` | run `f` and get its result or exception later with `get()` |
| `std::promise<R>` / `std::packaged_task<R()>` | produce a future's value yourself, for example in a pool |
| `std::latch`, `std::barrier` (C++20) | wait until N threads reach a point |
| `std::counting_semaphore` (C++20) | limit concurrent access to N |
| `std::atomic<T>::wait/notify` (C++20) | block on an atomic changing |
| `std::stop_source` / `stop_token` | cooperative cancellation |

## Finding races: ThreadSanitizer

A race may pass a thousand runs and fail in production. **ThreadSanitizer**
(`-fsanitize=thread`) watches every access while the tests run and reports both conflicting
stacks:

```text
WARNING: ThreadSanitizer: data race
  Write of size 4 by thread T2: #0 Counter::add counter.cpp:9
  Previous read of size 4 by thread T1: #0 Counter::total counter.cpp:13
```

It only sees code paths the tests run, and costs 5–15× in speed; run it in CI on tests that
exercise concurrency. The dojo's concurrency kata runs under it.

## Summary

- A data race is undefined behaviour; synchronise every shared write.
- `std::jthread` joins automatically and supports stop tokens.
- Lock with RAII (`lock_guard`, `scoped_lock`); keep the mutex with its data.
- Condition variables: wait with a predicate, change state under the lock, then notify.
- Atomics for single variables; seq_cst by default, acquire/release for publishing.
- Futures, latches, semaphores for higher-level coordination; TSan to find races.

## Further reading

- cppreference, Concurrency support library: <https://en.cppreference.com/w/cpp/thread>
- cppreference, std::memory_order: <https://en.cppreference.com/w/cpp/atomic/memory_order>
- cppreference, std::condition_variable: <https://en.cppreference.com/w/cpp/thread/condition_variable>
- C++ Core Guidelines, CP: Concurrency and parallelism: <https://isocpp.github.io/CppCoreGuidelines/CppCoreGuidelines#S-concurrency>
- Clang, ThreadSanitizer: <https://clang.llvm.org/docs/ThreadSanitizer.html>
- Anthony Williams, *C++ Concurrency in Action* (2nd edition)
- Herb Sutter, "atomic<> Weapons" (C++ and Beyond 2012)
