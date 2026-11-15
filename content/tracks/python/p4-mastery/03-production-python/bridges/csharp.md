---
from: csharp
status: draft
concept: "Operating Python next to .NET"
from_way: "ILogger<T> with Serilog sinks, appsettings.json plus environment variables and IOptions, dotnet publish into a chiseled container image, Kestrel using every core, health checks middleware and dotnet list package --vulnerable."
native_way: "logging.getLogger(__name__) with structured output, pydantic-settings from the environment, an image with a locked .venv, several worker processes, a health endpoint and pip-audit."
sensei_tip: "ILogger's message templates and Python's lazy %-style arguments solve the same problem: the value is kept as data and formatted only if the record is written. Do not pre-format with f-strings in log calls."
---

| .NET | Python |
| --- | --- |
| `ILogger<OrderService>` | `logging.getLogger(__name__)` |
| `logger.LogInformation("Charged {OrderId}", id)` | `log.info("charged %s", order_id)` |
| Serilog JSON sink | `structlog` / JSON formatter |
| `IOptions<T>` with environment variables | `BaseSettings` |
| `dotnet publish` + chiseled image | `uv sync --locked --no-dev` + slim image |
| Kestrel on all cores | several worker processes |
| `dotnet list package --vulnerable` | `pip-audit` |
| `IHostApplicationLifetime.ApplicationStopping` | SIGTERM handling, `lifespan` shutdown |
