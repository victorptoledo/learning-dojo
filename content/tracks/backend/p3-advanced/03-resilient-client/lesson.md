# Resilient Client

Wrap a flaky dependency the way Resilience4j and Spring's `@Retryable` do: retry transient failures
with capped exponential backoff, never retry permanent ones, and put a circuit breaker in front so a
dead dependency is not hammered. A fake clock and a recording sleeper make every timing exact in the
tests. Press **Enter** to create the kata folder and open it.
