---
from: java
status: draft
concept: "Request context without ThreadLocal"
from_way: "ThreadLocal and SLF4J's MDC hold the request id for the current thread; logback's JSON encoder writes it; Micrometer and OpenTelemetry's Java agent provide metrics and traces."
native_way: "AsyncLocalStorage holds the request context for the current async flow; pino's mixin adds it to each line; prom-client and the OpenTelemetry Node.js SDK provide metrics and traces."
sensei_tip: "AsyncLocalStorage is MDC that survives every await. Java's ScopedValue (for virtual threads) is the closest modern relative: a value bound for the duration of a call."
---

| Java | Node.js |
| --- | --- |
| `ThreadLocal<T>` / `ScopedValue<T>` | `AsyncLocalStorage<T>` |
| `MDC.put("requestId", id)` | `als.run({ requestId }, fn)` + logger mixin |
| logback JSON encoder | pino |
| Micrometer | prom-client |
| OpenTelemetry Java agent | OpenTelemetry Node.js SDK (`--import`) |
