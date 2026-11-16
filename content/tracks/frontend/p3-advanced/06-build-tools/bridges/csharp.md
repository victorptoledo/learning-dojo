---
from: csharp
status: draft
concept: "Build pipelines"
from_way: "dotnet build and publish with trimming; ASP.NET Core's static web assets and bundling."
native_way: "The frontend build does trimming (tree shaking), bundling, fingerprinting (hashed names) and compression."
sensei_tip: "ASP.NET Core's asp-append-version fingerprints files; bundlers do the same for every asset, which is what makes immutable caching safe."
---

| C# | In the browser |
| --- | --- |
| `dotnet publish -p:PublishTrimmed=true` | Tree shaking |
| `asp-append-version` | Content-hashed file names |
| NuGet packages | npm packages |
| appsettings secrets | Never in the frontend bundle |
