---
from: java
status: reviewed
concept: "From JVM tuning to end-to-end performance"
from_way: "Profiling the JVM and tuning GC while the page is slow because of a 2 MB JavaScript bundle."
native_way: "Measure the whole path first (Web Vitals, traces), then fix the biggest cost; for live updates use SseEmitter or WebSockets with a broker for fan-out."
sensei_tip: "Spring MVC's SseEmitter (or a Flux of ServerSentEvent in WebFlux) gives you SSE in a few lines; virtual threads make many open connections cheap."
---

| Java | Performance and real-time |
| --- | --- |
| `SseEmitter` | Server-Sent Events |
| `@EnableWebSocketMessageBroker` + STOMP relay | WebSocket fan-out through a broker |
| `CompletableFuture.allOf` | parallel downstream calls |
| HikariCP `maximumPoolSize` | the database pool size |
