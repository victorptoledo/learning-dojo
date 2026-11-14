# Virtual Threads (Project Loom)

A typical backend request spends most of its time **waiting**: for the database, for another
service, for a file. In classic Java each request holds a **platform thread** while it waits,
and platform threads are expensive (an OS thread each, about 1 MB of stack). With a pool of
200 threads, request 201 waits in a queue even though the CPU is idle.

The industry answer for a decade was asynchronous code: callbacks, `CompletableFuture`
chains, reactive streams. They scale, but they are hard to write, read and debug. Java 21's
answer is different: make threads **cheap**, and keep writing simple blocking code.

## What a virtual thread is

A **virtual thread** is a `Thread` managed by the JVM instead of the operating system. The
JVM runs many virtual threads on a few **carrier** threads (by default one per CPU core).
When a virtual thread blocks on I/O, the JVM **unmounts** it from its carrier, parks its
small stack on the heap, and runs another virtual thread on that carrier. When the I/O
completes, the virtual thread is **mounted** again, on any free carrier, and continues.

| | Platform thread | Virtual thread |
| --- | --- | --- |
| Backed by | one OS thread | the JVM, on a few carrier threads |
| Creation cost | high (OS call, ~1 MB stack) | tiny (a few hundred bytes to start) |
| How many | thousands | millions |
| Blocking I/O | holds the OS thread | unmounts; the carrier runs others |
| Pool them? | yes | **no**: create one per task |

Your code does not change: `Thread.sleep`, `InputStream.read`, JDBC calls, `HttpClient.send`
and `BlockingQueue.take` all unmount a virtual thread instead of blocking the carrier.

## Starting virtual threads

```java
Thread thread = Thread.ofVirtual().name("worker").start(() -> handle(request));
thread.join();

Thread.startVirtualThread(() -> handle(request));   // the short form
```

In practice you use an **executor that starts a new virtual thread per task**:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (Request request : requests) {
        executor.submit(() -> handle(request));
    }
}   // close() waits for every task to finish
```

`ExecutorService` is `AutoCloseable` since Java 19: leaving the `try` block waits for the
submitted tasks. Ten thousand tasks that each sleep one second finish in about one second:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> executor.submit(() -> {
        Thread.sleep(Duration.ofSeconds(1));
        return i;
    }));
}
```

With a fixed pool of 200 platform threads the same code takes about 50 seconds.

### Getting results back

`submit` returns a `Future`; `get()` blocks (cheaply, on a virtual thread) until the result
is ready, and `get(timeout, unit)` gives up after a while:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Price> price = executor.submit(() -> pricing.fetch(sku));
    Future<Stock> stock = executor.submit(() -> inventory.fetch(sku));
    return new Offer(price.get(), stock.get());   // both calls ran at the same time
}
```

## When virtual threads help, and when they do not

**They help** when you have many concurrent tasks that mostly **wait**: web requests,
database queries, calls to other services, message consumers. Throughput grows with the
number of tasks instead of the size of a pool.

**They do not help** with **CPU-bound** work. Computing a hash or resizing an image keeps a
carrier busy; you still have only as many carriers as cores. Use a fixed pool or parallel
streams for that.

## Pitfalls

- **Do not pool virtual threads.** Pools exist to limit an expensive resource. Virtual
  threads are not expensive; create one per task and throw it away.
- **Limit the real resource instead.** If the database accepts 20 connections, ten thousand
  virtual threads will queue on the connection pool, which is correct. To limit access to
  something without a pool, use a `Semaphore`:

  ```java
  private final Semaphore permits = new Semaphore(20);

  Result call() throws InterruptedException {
      permits.acquire();
      try {
          return remote.call();
      } finally {
          permits.release();
      }
  }
  ```
- **ThreadLocal caches.** Code that caches expensive objects in a `ThreadLocal` assumed a
  few long-lived threads. With a million short-lived virtual threads each gets its own copy.
  Prefer passing values, or `ScopedValue` (next lesson).
- **Pinning.** A virtual thread is *pinned* to its carrier when it cannot unmount, for
  example while running native code. Before Java 24, blocking inside a `synchronized` block
  also pinned it; since Java 24 `synchronized` no longer pins. If you still run on Java 21,
  prefer `ReentrantLock` around blocking calls. The Flight Recorder event
  `jdk.VirtualThreadPinned` shows where pinning still happens.

> [!TIP]
> In frameworks the switch is often one setting. Spring Boot runs request handling on
> virtual threads with `spring.threads.virtual.enabled=true`.

## Inspecting them

`Thread.currentThread().isVirtual()` tells you which kind you are on. Thread dumps include
virtual threads with `jcmd <pid> Thread.dump_to_file -format=json dump.json`. Debuggers in
IntelliJ and VS Code show them like any other thread.

## Summary

- Virtual threads are cheap JVM-managed threads that unmount while they block, so
  blocking code scales to millions of concurrent tasks.
- Use `Executors.newVirtualThreadPerTaskExecutor()` in a try-with-resources block, one
  task per thread, and `Future.get` to collect results.
- They speed up waiting, not computing.
- Do not pool them; limit the scarce resource with its own pool or a `Semaphore`; watch
  `ThreadLocal` use and pinning.
