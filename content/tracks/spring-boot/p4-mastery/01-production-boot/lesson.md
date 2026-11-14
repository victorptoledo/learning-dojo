# Spring Boot in Production

A Boot application in production is usually one executable JAR in a container, configured by its
environment, watched by probes and metrics, and replaced by a new version several times a week.
This lesson is the checklist for getting there safely.

## The artifact

`mvn package` builds an **executable fat JAR**: your classes, every dependency and a launcher.
`java -jar app.jar` runs it with an embedded server. Boot also writes the JAR in **layers**
(dependencies, Spring Boot loader, snapshot dependencies, application), so a container image can
put each in its own layer and a code change only rebuilds the small top one.

### Container images

Two good options:

1. **Buildpacks**, no Dockerfile: `mvn spring-boot:build-image` produces an OCI image with a
   JRE, the layered JAR and sensible memory settings.
2. A **Dockerfile** that extracts the layers:

```dockerfile
FROM eclipse-temurin:25-jre AS extract
WORKDIR /app
COPY target/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
RUN useradd --uid 10001 app
USER app
WORKDIR /app
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

Run as a non-root user, use a JRE base image of a pinned version, and let the JVM size its heap
from the container limit.

## Configuration per environment

The same image goes to every environment; only configuration changes (see the configuration
lesson). In containers:

- set values as **environment variables** (`SPRING_DATASOURCE_URL`, `SHOP_CURRENCY`);
- mount **secrets** as files and read them with `spring.config.import=configtree:/run/secrets/`,
  or use your platform's secret manager integration;
- keep `application.yaml` defaults safe for production (no demo users, no `ddl-auto=create`), and
  put developer conveniences in a `dev` profile.

## Databases: migrations, not ddl-auto

In production the schema changes through versioned migration scripts that run before the new
version starts serving:

```text
src/main/resources/db/migration/
    V1__create_products.sql
    V2__add_stock_column.sql
```

With **Flyway** on the classpath (`spring-boot-starter-flyway`), Boot applies pending migrations
at startup. Set `spring.jpa.hibernate.ddl-auto=validate` so Hibernate checks the entities against
the real schema instead of changing it. Write migrations that work with both the old and the new
version of the code (add a column first, use it in the next release, drop the old one later), so a
rolling deploy never breaks.

## Probes and graceful shutdown

The orchestrator needs two answers from the actuator:

| Probe | Question | On failure |
| --- | --- | --- |
| **liveness** `/actuator/health/liveness` | is the process stuck? | restart it |
| **readiness** `/actuator/health/readiness` | can it take traffic now? | stop routing requests to it |

On shutdown (`SIGTERM`), Boot shuts down **gracefully** by default: readiness goes to
`REFUSING_TRAFFIC`, the server stops accepting new requests, and in-flight requests get time to
finish:

```properties
spring.lifecycle.timeout-per-shutdown-phase=20s
```

Make the orchestrator's termination grace period longer than that.

## Performance defaults worth setting

```properties
spring.threads.virtual.enabled=true      # request handling and @Async on virtual threads
spring.jpa.open-in-view=false            # no lazy loading during view rendering
server.compression.enabled=true          # gzip large JSON responses
spring.datasource.hikari.maximum-pool-size=20
```

Size the connection pool from the **database's** capacity, not from the number of requests:
with virtual threads, thousands of requests will queue for a few connections, which is correct.

## Security checklist

- Actuator: expose only `health` publicly; everything else behind authentication or on a
  management port.
- Errors: Problem Details without stack traces (`server.error.include-stacktrace=never` is the
  default; keep it).
- Dependencies: keep Spring Boot on a supported version and patch regularly; the BOM upgrades the
  whole stack together.
- TLS usually ends at the load balancer; set `server.forward-headers-strategy=framework` so
  Spring sees the original scheme and host.

## Releasing

- Build once, promote the same image through environments.
- Deploy gradually (rolling or canary), watch error rate and latency, and roll back fast.
- Tag images with the version and the git commit; `/actuator/info` can show both
  (`spring-boot-maven-plugin` `build-info` goal and `git-commit-id-maven-plugin`).

## Summary

- Ship the layered executable JAR in a small, non-root image (buildpacks or an extracted-layers
  Dockerfile).
- One image for all environments; configuration and secrets from the environment.
- Flyway migrations with `ddl-auto=validate`, written to be compatible across one release.
- Liveness and readiness probes, graceful shutdown with a timeout.
- Virtual threads, `open-in-view=false`, a pool sized for the database, and a locked-down
  actuator.
