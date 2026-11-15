---
from: java
status: draft
concept: "Observer, command and chain of responsibility"
from_way: "Services that call every interested party directly; `java.util.Observable` subclasses; undo by copying the whole state."
native_way: "Functional listener interfaces with subscription handles, Spring events, commands as small classes or records, `Runnable` and executors as invokers, and pipelines of lambda steps."
sensei_tip: "Spring Security is a chain of responsibility you configure but rarely see: `SecurityFilterChain` is an ordered list of filters, and debugging means finding which link stopped the request."
---

| Java | Pattern |
| --- | --- |
| `@EventListener`, `ApplicationEventPublisher` | observer |
| `Flow.Subscriber`, Project Reactor | observer with back-pressure |
| `Runnable`, `ExecutorService.submit` | command and invoker |
| Servlet `Filter`, `HandlerInterceptor` | chain of responsibility |
| `Function.andThen`, `List<UnaryOperator<T>>` | pipeline |
