---
from: csharp
status: draft
concept: "A declarative build recipe"
from_way: "The .csproj and dotnet publish describe the build; launchSettings.json and the hosting environment describe how it runs."
native_way: "The Dockerfile restores, publishes and declares ENTRYPOINT [\"dotnet\", \"App.dll\"], with configuration from ENV such as ASPNETCORE_URLS or ASPNETCORE_HTTP_PORTS."
sensei_tip: "Recent .NET images listen on port 8080 and run as a non-root app user by default; keep both and EXPOSE 8080."
---

| C# / .NET | Dockerfile |
| --- | --- |
| `dotnet restore` | `RUN dotnet restore` after copying only the .csproj |
| `dotnet publish -o out` | `RUN dotnet publish -c Release -o /out` |
| `launchSettings.json` | `ENV` and `EXPOSE` |
| `.gitignore` for `bin/` and `obj/` | `.dockerignore` with `bin/` and `obj/` |
