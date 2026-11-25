# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): an exchange-rate desk that works but has none
   of the cross-cutting rules yet. Make reads public and cached, let only traders change a
   rate (on the service method, not just the URL), audit and count every change **after it
   commits**, and add a health check that goes `DOWN` when a rate is too old.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Spring Security | Which requests are public? `401` or `403`? Why put the rule on the method too? |
| Secured Notes (kata) | How do the tests log in, and how does `@WithMockUser` work without HTTP? |
| Actuator, Metrics and Tracing | How do I write my own `HealthIndicator`? What do a counter's tags give me? |
| Events, Async Work, Scheduling and Caching | What happens to an `@EventListener` when the transaction rolls back? What does `@CacheEvict` clear, and when? |

> [!TIP]
> Every piece of the kata is an annotation or a bean that Spring wires around your code through
> a proxy. If a test says nothing happened, ask whether the call went through the proxy.
