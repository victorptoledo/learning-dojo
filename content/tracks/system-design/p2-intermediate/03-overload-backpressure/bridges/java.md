---
from: java
status: draft
concept: "Staying useful under overload"
from_way: "A ThreadPoolExecutor with an unbounded LinkedBlockingQueue and an HTTP client with default timeouts."
native_way: "Bounded pools and queues that reject when full, timeouts on every call, retries with backoff and jitter, and shedding by priority."
sensei_tip: "Executors.newFixedThreadPool uses an unbounded queue: under overload it buffers forever. Build a ThreadPoolExecutor with an ArrayBlockingQueue and a RejectedExecutionHandler, and map rejections to 503 with Retry-After."
---

| Java | Overload concept |
| --- | --- |
| `new ArrayBlockingQueue<>(1000)` + `offer` | a bounded queue that pushes back |
| `ThreadPoolExecutor.AbortPolicy` | reject when full (load shedding) |
| `HttpClient.newBuilder().connectTimeout(...)` / `HttpRequest.timeout(...)` | connect and request timeouts |
| Resilience4j `Retry` with `IntervalFunction.ofExponentialRandomBackoff` | backoff with jitter |
| Resilience4j `Bulkhead` / `CircuitBreaker` | concurrency limits, fail fast |
| `Semaphore(50)` around a dependency | a bulkhead in one line |
