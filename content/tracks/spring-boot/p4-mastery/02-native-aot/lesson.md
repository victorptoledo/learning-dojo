# AOT, Native Images and Fast Startup

A Spring Boot service on the JVM starts in a few seconds and warms up over the first minute.
For a long-running service that is fine. For scale-to-zero, serverless functions, CLIs or fast
autoscaling it is not. Spring offers three ways to start faster, with different costs.

## Why Spring starts slowly

At startup Spring scans the classpath, reads annotations with reflection, evaluates hundreds of
auto-configuration conditions, creates bean definitions and proxies, and only then creates the
beans. Then the JVM interprets the code until the JIT has compiled the hot paths. Most of that
work produces the same result every time.

## Spring AOT processing

**Ahead-of-time processing** moves part of that work to build time. During the build, Spring
starts the application context in a special mode, evaluates the conditions **once**, and
generates:

- Java source code that registers the bean definitions directly (no scanning, no condition
  evaluation at run time);
- **reachability metadata** (reflection, resources, proxies) for GraalVM.

The trade-off is a **closed world** at build time: the classpath and the bean configuration are
fixed. `@Profile` and `@ConditionalOnProperty` choices are decided when you build, not when you
run, so build a native image per configuration that changes which beans exist.

## Option 1: GraalVM native image

```text
mvn -Pnative native:compile          # needs GraalVM installed
mvn -Pnative spring-boot:build-image # or build a native container image with buildpacks
```

The result is a native executable that starts in **tens of milliseconds** and uses a fraction of
the memory. What to expect:

| | JVM | Native image |
| --- | --- | --- |
| Startup | seconds | ~50–100 ms |
| Memory at rest | higher | much lower |
| Peak throughput | best after warm-up | often a little lower |
| Build | seconds | minutes, several GB of RAM |
| Libraries | everything | need reachability metadata (most popular ones have it) |

Write code the closed world can see: constructor injection, no `Class.forName` with computed
names, no reflection on classes Spring does not know about. When you must use reflection, declare
it with a `RuntimeHintsRegistrar` or `@RegisterReflectionForBinding(MyDto.class)`.

Test the native build in CI (`mvn -PnativeTest test`): some problems only show up in the image.

## Option 2: the JVM with an AOT cache

Since Java 24, the JVM itself can cache class loading and linking, and since Java 25 also method
profiles, from a **training run**. Spring Boot supports it directly:

```text
java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar app.jar   # training run
java -XX:AOTCache=app.aot -jar app.jar                                         # every later start
```

You keep the normal JVM (full tooling, best peak performance, any library) and typically cut
startup time substantially. Combined with Spring AOT (`-Dspring.aot.enabled=true` on a JAR built
with AOT processing), the gain is larger. The cache must be created with the same JDK and the same
JAR it is used with.

## Option 3: checkpoint and restore (CRaC)

**CRaC** snapshots a warmed-up JVM process and restores it later in milliseconds, with the JIT
already done. It needs a JDK with CRaC support and an operating system that allows it, and
resources such as open sockets must be closed before the checkpoint and reopened after restore
(Spring handles its own). Powerful, but the most operationally demanding.

## Choosing

| Situation | Choose |
| --- | --- |
| Long-running service, steady traffic | plain JVM (maybe with the AOT cache) |
| Faster restarts and autoscaling, no build changes | JVM + AOT cache |
| Scale to zero, serverless, CLIs, tight memory | GraalVM native image |
| Instant start of a warm JVM, and you control the platform | CRaC |

Measure startup and throughput for **your** service before deciding; the differences depend a
lot on the libraries you use.

## Summary

- Spring AOT moves bean definition work to build time and generates metadata, at the cost of a
  configuration fixed at build time.
- GraalVM native images start in milliseconds and use little memory, with longer builds and some
  limits on reflection.
- The JVM AOT cache (Java 24+) speeds up startup while keeping the JVM.
- CRaC restores a warm JVM snapshot.
- Pick by workload, and measure.
