# Production Java: Logs, Config and Packaging

A service is ready for production when the people running it can **see** what it does,
**configure** it without rebuilding, and **ship** it the same way every time. This lesson
covers the plain-Java version of each; frameworks such as Spring Boot (next track) automate
most of it on the same ideas.

## Logging

### Which API

The common setup is **SLF4J** as the API your code calls, with **Logback** or Log4j 2 as the
backend that writes the output:

```java
private static final Logger log = LoggerFactory.getLogger(OrderService.class);

log.info("order {} placed by {}", order.id(), customer.id());
log.warn("payment retry {} for order {}", attempt, order.id());
log.error("payment failed for order {}", order.id(), exception);   // exception last: stack trace
```

Libraries that must not pick a backend can use the JDK's own `System.Logger`
(`System.getLogger(name)`), which frameworks route to the backend you chose.

### What good logs look like

- **Placeholders, not concatenation**: `log.debug("x {}", expensive())` still calls
  `expensive()`, but the string is only built when the level is enabled. For costly
  arguments, use `log.atDebug().addArgument(() -> expensive()).log("x {}")` or check
  `log.isDebugEnabled()`.
- **Levels with meaning**: `ERROR` something failed and someone should look; `WARN`
  unexpected but handled; `INFO` important business events (started, order placed);
  `DEBUG` details for diagnosing; `TRACE` very detailed.
- **Identifiers, not prose**: order id, user id, request id. Add a correlation id to every
  line of a request with the MDC: `MDC.put("requestId", id)`.
- **Structured output** (JSON) in production, so the log platform can search fields.
- **No secrets**: never log passwords, tokens, card numbers or full personal data.
- Log an exception **once**, where it is handled, with its stack trace. Logging and
  rethrowing at every layer fills the logs with duplicates.

## Configuration

Follow the twelve-factor rule: **configuration lives in the environment**, not in the code.
The same artifact runs in test and production with different settings.

```java
record AppConfig(int port, URI databaseUrl, Duration timeout) {
    static AppConfig fromEnvironment(Map<String, String> env) {
        return new AppConfig(
                Integer.parseInt(env.getOrDefault("PORT", "8080")),
                URI.create(required(env, "DATABASE_URL")),
                Duration.parse(env.getOrDefault("TIMEOUT", "PT5S")));
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing configuration: " + key);
        }
        return value;
    }
}

var config = AppConfig.fromEnvironment(System.getenv());
```

- **Fail fast at startup**: read and validate every setting before serving traffic. A typo
  should stop the deploy, not break the first request at 3 a.m.
- Use **typed values** (`Duration`, `URI`, `int`) in one record, not strings read all over
  the code.
- **Secrets** come from the environment or a secret manager, never from the repository.
- Passing `System.getenv()` in as a `Map` keeps the code testable.

## Packaging

| Artifact | How | When |
| --- | --- | --- |
| **Runnable JAR** | `mvn package` with a `Main-Class`; frameworks build a "fat" JAR with dependencies | the default |
| **Custom runtime** | `jlink --add-modules java.base,java.net.http --output runtime` | a small JDK with only the modules you need |
| **Installer** | `jpackage` | desktop apps |
| **Container image** | a Dockerfile, or `mvn spring-boot:build-image` / Jib | services |
| **Native executable** | GraalVM `native-image` | fast startup, see the previous lesson |

A minimal, reproducible container image:

```dockerfile
FROM eclipse-temurin:25-jre
RUN useradd --uid 10001 app
USER app
WORKDIR /app
COPY target/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Run as a **non-root** user, pin the base image version, copy only the JAR, and let the JVM
size itself from the container limits.

## Observability: logs, metrics, traces

- **Metrics** answer "how much and how fast": request rate, error rate, latency percentiles,
  JVM memory and GC. Micrometer is the usual library; Prometheus scrapes it.
- **Traces** follow one request across services. **OpenTelemetry** is the standard; its Java
  agent instruments common libraries with no code change:
  `java -javaagent:opentelemetry-javaagent.jar -jar app.jar`.
- **Health checks**: an endpoint that says whether the process is alive and whether it is
  ready for traffic, so the orchestrator can restart or hold it.

## Shutting down cleanly

On `SIGTERM` the service should stop accepting work, finish what is in flight and close its
resources. A shutdown hook covers plain Java:

```java
Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
    server.stop(Duration.ofSeconds(20));
    pool.close();
}));
```

## Summary

- Log through SLF4J with placeholders, meaningful levels, ids, JSON output and no secrets;
  log each exception once.
- Read configuration from the environment into one typed record and fail fast on bad values.
- Ship a runnable JAR in a small, non-root container image; `jlink` and native images make
  it smaller.
- Add metrics, traces and health checks, and shut down gracefully.
