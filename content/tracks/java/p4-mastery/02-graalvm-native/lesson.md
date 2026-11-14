# GraalVM Native Image: Millisecond Startup

A JVM application starts by loading classes, interpreting bytecode and warming up the JIT.
For a long-running server that cost disappears in the first minute. For a command-line tool,
a serverless function or a service that scales from zero, it is the whole run. **GraalVM
Native Image** compiles the application ahead of time (AOT) into a standalone executable
that starts in milliseconds.

## How it works

`native-image` takes your JAR and its dependencies and performs a **closed-world analysis**:
starting from `main`, it finds every class, method and field that can be reached, compiles
only those to machine code, and runs static initialisers at build time where it can. The
result is one executable with its own small runtime (the *Substrate VM*): no JDK needed on
the target machine.

```text
native-image -jar app.jar app        # or: mvn -Pnative native:compile
./app                                # starts in tens of milliseconds
```

## What you gain, what you pay

| | JVM (JIT) | Native image (AOT) |
| --- | --- | --- |
| Startup | hundreds of ms to seconds | milliseconds |
| Memory at rest | higher (JIT, metadata) | much lower |
| Peak throughput | best: the JIT optimises with run-time profiles | often somewhat lower (profile-guided optimisation narrows the gap) |
| Build time | seconds | minutes, with gigabytes of RAM |
| Dynamic features | everything | reflection, proxies and resources need metadata |
| Debugging, profiling | full JVM tooling | more limited |

> [!NOTE]
> Native images shine for CLIs, serverless functions and services that scale to zero.
> A long-running, high-traffic service usually does as well or better on the JVM after
> warm-up.

## The closed-world catch: reflection and resources

Anything the analysis cannot see statically is missing from the image: classes loaded by
name, reflection, dynamic proxies, resources read from the classpath, serialization. The fix
is **reachability metadata**, JSON files under `META-INF/native-image/` that list them:

```json
{
  "reflection": [
    { "type": "shop.OrderDto", "allDeclaredFields": true, "allDeclaredConstructors": true }
  ],
  "resources": [
    { "glob": "templates/*.html" }
  ]
}
```

You rarely write it by hand:

- Frameworks generate it. **Spring Boot**, Quarkus and Micronaut have AOT steps that produce
  the metadata for their own features.
- The **tracing agent** records what your application uses while it runs on the JVM:

  ```text
  java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar app.jar
  ```

- The shared **GraalVM Reachability Metadata Repository** covers many popular libraries, and
  the Maven and Gradle plugins use it automatically.

Code you write yourself is easiest to compile natively when it avoids reflection: records,
explicit constructors and plain method calls are visible to the analysis.

## Build-time initialisation

Static initialisers that can run safely at build time are executed then, and their results
are stored in the image heap. That is part of why startup is so fast. Code that must run at
startup (reading the environment, opening a socket, seeding a random generator) has to stay
**run-time initialised**, or the image would contain the build machine's values.

## Testing and CI

- Run your tests **in native mode** too (`mvn -PnativeTest test`): reflection problems show up
  only in the image.
- Build in CI on the **target OS**: an executable built on Windows runs only on Windows. For
  containers, build inside a Linux builder image.
- Watch build memory: a native build of a Spring Boot service may need several GB.

## Choosing between JIT, AOT cache and native

| Need | Choose |
| --- | --- |
| Best peak throughput, full tooling | JVM with the JIT |
| Faster startup, same JVM behaviour | JVM with the AOT cache (`-XX:AOTCache`, Java 24+) or CRaC checkpoints |
| Milliseconds of startup, small memory, no JDK on the target | GraalVM native image |

## Summary

- Native Image compiles the reachable code ahead of time into a standalone executable with
  millisecond startup and low memory.
- The closed-world analysis needs metadata for reflection, proxies and resources; frameworks
  and the tracing agent generate it.
- You pay with long builds, possibly lower peak throughput and weaker tooling.
- Use it for CLIs, serverless and scale-to-zero services; stay on the JVM for long-running,
  high-throughput servers.
