---
from: csharp
status: draft
concept: "Shipping Java today"
from_way: "ILogger, IOptions, dotnet publish into an aspnet image, and OpenTelemetry."
native_way: "SLF4J, config from the environment, JAR in a JRE image, Micrometer and OpenTelemetry."
sensei_tip: "Java's logging story is split: SLF4J is the API (like Microsoft.Extensions.Logging), Logback or Log4j 2 the implementation (like Serilog)."
---

| C# | Java 25 |
| --- | --- |
| ILogger<T> | SLF4J Logger |
| Serilog | Logback |
| IOptions<T> | typed configuration |
| mcr.microsoft.com/dotnet/aspnet | eclipse-temurin:25-jre |
| OpenTelemetry .NET | OpenTelemetry Java agent |
