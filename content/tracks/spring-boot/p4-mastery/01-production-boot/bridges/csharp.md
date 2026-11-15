---
from: csharp
status: draft
concept: "Shipping a Spring Boot service"
from_way: "dotnet publish into an aspnet image, appsettings per environment and EF migrations."
native_way: "Buildpack or layered images, profiles and environment variables, Flyway, probes."
sensei_tip: "dotnet publish /t:PublishContainer is mvn spring-boot:build-image: both build an OCI image without a Dockerfile."
---

| C# | Spring Boot 4 |
| --- | --- |
| PublishContainer | spring-boot:build-image |
| ASPNETCORE_ENVIRONMENT | SPRING_PROFILES_ACTIVE |
| EF migrations bundle | Flyway |
| health check endpoints | Actuator probes |
| HostOptions.ShutdownTimeout | spring.lifecycle.timeout-per-shutdown-phase |
