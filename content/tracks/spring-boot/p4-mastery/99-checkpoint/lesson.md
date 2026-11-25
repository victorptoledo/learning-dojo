# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards running
   Boot in production, AOT and application architecture. You need 7 right.
2. **The integration kata** (press **Enter**): an invoicing service that passed its demo and now
   faces the production review. Its settings are read with defaults that hide mistakes,
   Actuator shows everything to everyone, there are no probes and no way to drain it, its two
   modules call into each other's internals, and the formatter it loads by reflection would
   vanish from a native image. Fix each review point so a test proves it.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Configuration (P1) | Why is a typed, validated `@ConfigurationProperties` safer than `@Value` with a default? |
| Spring Boot in Production | Which Actuator endpoints does the platform need, and who may see the details? What is the difference between liveness and readiness? |
| AOT, Native Images and Fast Startup | What does a native image drop, and how do I tell it what my code loads by name? |
| Application Architecture with Spring | What may another module use from mine? Why an event after commit instead of a direct call? |
| Events, Async Work, Scheduling and Caching (P3) | Which listener runs only once the transaction committed? |

> [!TIP]
> Each review point is checked by its own grading test class: `StartupGradingTest` (settings),
> `ProductionGradingTest` (Actuator, probes, maintenance and the invoice flow) and
> `ArchitectureGradingTest` (module boundaries and runtime hints). Fix one class at a time.
