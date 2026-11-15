---
from: csharp
status: draft
concept: "From a web framework to Spring Boot 4"
from_way: "ASP.NET Core with WebApplication.CreateBuilder, built-in DI and appsettings.json."
native_way: "Spring Boot with SpringApplication.run, its IoC container and application.properties."
sensei_tip: "ASP.NET Core borrowed a lot from Spring: builder.Services is the bean registry, Program.cs is the main class with @SpringBootApplication, and appsettings.json is application.yml."
---

| C# | Spring Boot 4 |
| --- | --- |
| dotnet new webapi | start.spring.io |
| WebApplication.CreateBuilder | SpringApplication.run |
| builder.Services.Add... | beans and auto-configuration |
| appsettings.json | application.properties / .yml |
| Kestrel | embedded Tomcat |
