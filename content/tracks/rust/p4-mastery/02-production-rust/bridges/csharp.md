---
from: csharp
status: draft
concept: "Running in production"
from_way: "ILogger with Serilog, IOptions configuration, and dotnet publish into an aspnet image."
native_way: "tracing, typed config structs, error enums and a static binary."
sensei_tip: "IOptions<T> with validation maps onto a serde struct you validate at startup. A Rust binary is like a .NET Native AOT publish: no runtime in the image, fast startup."
---

| C# | Rust |
| --- | --- |
| ILogger<T> | tracing spans and events |
| IOptions<T> | a config struct (serde) |
| custom exceptions | thiserror |
| Native AOT publish | cargo build --release |
| OpenTelemetry .NET | tracing-opentelemetry |
