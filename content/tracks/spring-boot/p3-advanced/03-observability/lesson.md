# Actuator, Metrics and Tracing

When a service misbehaves in production you cannot attach a debugger. You need it to tell you,
continuously, **whether it is healthy**, **how much and how fast** it works, and **what one
request went through**. Spring Boot covers all three: Actuator for health and management,
Micrometer for metrics and observations, and OpenTelemetry-compatible tracing.

## Actuator

Add `spring-boot-starter-actuator` and the application gets management endpoints under
`/actuator`. Only `health` is exposed over HTTP by default; expose what you need, deliberately:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.probes.enabled=true
management.endpoint.health.show-details=when-authorized
```

| Endpoint | Shows |
| --- | --- |
| `/actuator/health` | `UP` or `DOWN`, aggregated from the database, disk space and your own checks |
| `/actuator/health/liveness`, `/readiness` | the probes an orchestrator (Kubernetes) uses |
| `/actuator/info` | build version, git commit, anything you add |
| `/actuator/metrics` | every metric name; `/actuator/metrics/http.server.requests` for one |
| `/actuator/prometheus` | all metrics in Prometheus format (needs `micrometer-registry-prometheus`) |
| `/actuator/loggers` | read and change log levels at run time |

> [!WARNING]
> Endpoints such as `env`, `heapdump`, `threaddump` and `loggers` reveal internals or change
> behaviour. Never expose them publicly: protect `/actuator/**` with Spring Security, or serve
> management on a separate port (`management.server.port=8081`) reachable only internally.

### Your own health check

```java
@Component
class PaymentProviderHealth implements HealthIndicator {
    private final PaymentClient client;

    PaymentProviderHealth(PaymentClient client) { this.client = client; }

    @Override
    public Health health() {
        return client.ping()
                ? Health.up().build()
                : Health.down().withDetail("provider", "not answering").build();
    }
}
```

Think about what "down" should mean: if the readiness probe fails because an optional
dependency is slow, the orchestrator stops sending traffic to a service that could still work.

## Metrics with Micrometer

Micrometer is to metrics what SLF4J is to logging: one API, many backends (Prometheus, OTLP,
Datadog...). Boot already records HTTP requests, JVM memory and GC, the connection pool, caches
and more. Add business metrics where they matter:

```java
@Service
class CheckoutService {
    private final Counter orders;
    private final Timer payment;

    CheckoutService(MeterRegistry registry) {
        this.orders = Counter.builder("shop.orders.placed").tag("channel", "web").register(registry);
        this.payment = Timer.builder("shop.payment.duration").register(registry);
    }

    void checkout(Cart cart) {
        payment.record(() -> pay(cart));
        orders.increment();
    }
}
```

| Type | For |
| --- | --- |
| `Counter` | things that only go up: orders, errors |
| `Gauge` | a current value: queue size, items in a cache |
| `Timer` | how long something takes, with counts and percentiles |
| `DistributionSummary` | sizes: bytes, items per order |

Keep **tag values bounded**: `status=200` is fine, `userId=...` creates one time series per user
and can take down your metrics backend.

## Observations: one API for metrics and traces

The Observation API records an operation once and produces both a metric and a trace span:

```java
@Observed(name = "shop.checkout", contextualName = "checkout")
public Receipt checkout(Cart cart) { ... }
```

or programmatically with `Observation.createNotStarted("shop.checkout", registry).observe(() -> ...)`.

## Tracing

A **trace** follows one request across services; each step is a **span** with timing and
attributes. Boot instruments incoming and outgoing HTTP, JDBC and messaging, and propagates the
trace context in headers (`traceparent`, W3C Trace Context) to the next service.

```properties
management.tracing.sampling.probability=0.1
management.opentelemetry.tracing.export.otlp.endpoint=http://otel-collector:4318/v1/traces
```

Sample a fraction of requests in production (100% only in development). Send spans to an
OpenTelemetry collector, and view them in Jaeger, Grafana Tempo or your vendor's tool.

## Logs that line up with traces

With tracing on, Boot puts the `traceId` and `spanId` into the logging context, so every log
line of a request carries them. Search the logs by trace id and you get exactly that request's
story. Enable **structured logging** for machines to read the fields:

```properties
logging.structured.format.console=ecs
```

## Summary

- Actuator exposes health, info, metrics and more; expose only what you need and protect the
  rest.
- Write `HealthIndicator`s for real dependencies and use liveness and readiness probes.
- Micrometer records metrics: counters, gauges, timers; keep tags low-cardinality.
- `@Observed` and the Observation API give metrics and spans from one place.
- Tracing propagates context between services; sample it, export it over OTLP, and correlate
  logs by trace id.
