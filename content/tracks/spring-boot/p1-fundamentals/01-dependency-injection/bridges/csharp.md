---
from: csharp
status: draft
concept: "Wiring objects"
from_way: "Microsoft.Extensions.DependencyInjection with AddSingleton, AddScoped and AddTransient."
native_way: "Spring's container with singleton beans by default and other scopes when needed."
sensei_tip: "AddSingleton is the default Spring scope; AddScoped matches request scope; Spring finds classes by scanning instead of explicit registration."
---

| C# | Spring Boot 4 |
| --- | --- |
| builder.Services.AddSingleton<IClock, SystemClock>() | @Bean Clock clock() |
| AddScoped | @RequestScope |
| constructor injection | constructor injection |
| IServiceProvider | ApplicationContext |
