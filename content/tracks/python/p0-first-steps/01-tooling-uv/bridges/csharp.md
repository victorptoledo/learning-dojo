---
from: csharp
status: draft
concept: "SDK, project file and restore"
from_way: "The .NET SDK, a .csproj with PackageReference items, dotnet restore into the shared NuGet cache and an optional packages.lock.json."
native_way: "uv installs the interpreter, pyproject.toml lists the dependencies, uv sync builds a .venv per project and uv.lock pins every version."
sensei_tip: "dotnet run and uv run feel the same: both restore what is missing, then run. Unlike NuGet, Python packages are installed per environment, so each project has its own copy."
---

| .NET | Python / uv |
| --- | --- |
| `dotnet --list-sdks` | `uv python list` |
| `.csproj` | `pyproject.toml` |
| `<PackageReference Include="Serilog" />` | `dependencies = ["structlog"]` |
| `dotnet restore` | `uv sync` |
| `packages.lock.json` | `uv.lock` |
| `dotnet run` / `dotnet test` | `uv run python main.py` / `uv run pytest` |
| `dotnet tool run` | `uvx` |
