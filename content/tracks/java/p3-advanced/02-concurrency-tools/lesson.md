# Executors, Futures and Thread Safety

Virtual threads make starting tasks cheap. This lesson is about everything around them:
waiting for several results, giving up in time, sharing state without corrupting it, and
passing context down a call chain.

## Futures: results that arrive later

`ExecutorService.submit` returns a `Future<T>`:

| Method | Does |
| --- | --- |
| `get()` | waits for the result; rethrows a task failure wrapped in `ExecutionException` |
| `get(timeout, unit)` | waits at most that long, then throws `TimeoutException` |
| `cancel(true)` | tries to stop the task (interrupts it) |
| `state()` (Java 19) | `RUNNING`, `SUCCESS`, `FAILED` or `CANCELLED` |
| `resultNow()`, `exceptionNow()` | the outcome of a finished task, without checked exceptions |

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Quote> quote = executor.submit(() -> supplier.quote(sku));
    try {
        return quote.get(2, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        quote.cancel(true);
        return Quote.unavailable();
    } catch (ExecutionException e) {
        throw new QuoteFailed(sku, e.getCause());   // the task's own exception
    }
}
```

`get` also throws `InterruptedException`: someone asked this thread to stop. Either rethrow
it or restore the flag with `Thread.currentThread().interrupt()`; never swallow it.

`invokeAll(tasks)` submits a list of `Callable`s and waits for all of them; the futures come
back **in the same order** as the tasks. `invokeAny(tasks)` returns the first successful
result and cancels the rest.

## CompletableFuture: composing asynchronous steps

`CompletableFuture` lets you chain steps without blocking a thread between them:

```java
CompletableFuture<Price> price = CompletableFuture.supplyAsync(() -> pricing.fetch(sku), executor);
CompletableFuture<Stock> stock = CompletableFuture.supplyAsync(() -> inventory.fetch(sku), executor);

Offer offer = price.thenCombine(stock, Offer::new)
        .orTimeout(2, TimeUnit.SECONDS)
        .exceptionally(error -> Offer.unavailable())
        .join();
```

`thenApply` transforms, `thenCompose` chains another future, `thenCombine` joins two,
`allOf` waits for many, `exceptionally` and `handle` recover. Pass your own executor; the
default is the shared `ForkJoinPool`, which is sized for CPU work.

With virtual threads, blocking code with `Future.get` is often simpler than a
`CompletableFuture` chain. Use `CompletableFuture` when you need its composition, or when an
API already returns one (`HttpClient.sendAsync`).

## Structured concurrency (preview)

Tasks started together should finish together: if one fails, the others are pointless.
`StructuredTaskScope` (a preview API in Java 25, enable with `--enable-preview`) expresses
that directly:

```java
try (var scope = StructuredTaskScope.open()) {
    Subtask<Price> price = scope.fork(() -> pricing.fetch(sku));
    Subtask<Stock> stock = scope.fork(() -> inventory.fetch(sku));
    scope.join();                       // waits; if one fails, the other is cancelled
    return new Offer(price.get(), stock.get());
}
```

The API is still changing between releases. Learn the idea, use the executor patterns in
production until it is final.

## Sharing state safely

Two threads that update the same variable without coordination produce **race conditions**:

```java
private int hits = 0;
void hit() { hits++; }   // read, add, write: two threads can lose updates
```

The tools, from simplest to most flexible:

| Tool | Use for |
| --- | --- |
| **Immutable objects** (records, `List.copyOf`) | data shared between threads: nothing to protect |
| **Confinement** | keep the state inside one task and return it as the result |
| `AtomicInteger`, `AtomicLong`, `LongAdder` | counters and single values: `hits.incrementAndGet()` |
| `ConcurrentHashMap` | shared maps: `counts.merge(word, 1, Integer::sum)` is atomic per key |
| `CopyOnWriteArrayList` | lists read very often and changed rarely (listeners) |
| `BlockingQueue` (`LinkedBlockingQueue`) | hand work from producers to consumers |
| `ReentrantLock` / `synchronized` | invariants that span several fields |

```java
private final ReentrantLock lock = new ReentrantLock();
private long balance;

void transfer(long cents) {
    lock.lock();
    try {
        if (cents > balance) throw new IllegalStateException("insufficient funds");
        balance -= cents;
    } finally {
        lock.unlock();
    }
}
```

> [!IMPORTANT]
> The best shared state is no shared state. Give each task its input, let it return its
> result, and combine the results on one thread. Most concurrency bugs disappear.

`volatile` makes writes to a field visible to other threads immediately, but it does not make
`x++` atomic. Use it for a simple flag such as `volatile boolean running`.

## Passing context: ScopedValue

A request often carries context (the user, a trace id) that deep methods need. The old answer
was a `ThreadLocal`, which is mutable, leaks if not removed, and is copied per thread. Java 25
finalised **scoped values**: an immutable value bound for the duration of a call.

```java
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

void handle(Request request) {
    ScopedValue.where(CURRENT_USER, authenticate(request))
            .run(() -> controller.process(request));
}

// anywhere below, on this thread:
User user = CURRENT_USER.get();
```

The binding ends when `run` returns, it cannot be changed, and it is cheap with millions of
virtual threads.

## Summary

- `Future.get(timeout)` waits with a limit; unwrap `ExecutionException.getCause()`; never
  swallow `InterruptedException`.
- `invokeAll` keeps task order; `CompletableFuture` composes steps (`thenCombine`,
  `orTimeout`, `exceptionally`) on an executor you choose.
- Structured concurrency groups related tasks (preview in Java 25).
- Prefer immutability and confinement; then atomics, `ConcurrentHashMap` and locks.
- Use `ScopedValue` instead of `ThreadLocal` to pass request context.
