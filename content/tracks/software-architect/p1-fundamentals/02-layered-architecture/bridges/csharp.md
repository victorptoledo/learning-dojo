---
from: csharp
status: draft
concept: "Layered architecture"
from_way: "Separate `Web`, `Business`, `Data` projects with every type public, and services that wrap `DbContext` calls one to one."
native_way: "Feature folders (vertical slices) with the endpoint, handler and data access together, `internal` types, and NetArchTest rules where layers are kept."
sensei_tip: "Vertical slice architecture (Jimmy Bogard) is package by feature in .NET clothes: one folder per request with its endpoint, handler and query."
---

| C# / ASP.NET Core | Layering idea |
| --- | --- |
| controllers, minimal API endpoints | presentation layer |
| application services, MediatR handlers | application layer |
| EF Core `DbContext`, repositories | persistence layer |
| project references Web → Business → Data | strict layers enforced by MSBuild |
| a feature folder with `internal` classes | package by feature (vertical slice) |
| a service that only calls `_db.Add` | sinkhole |
