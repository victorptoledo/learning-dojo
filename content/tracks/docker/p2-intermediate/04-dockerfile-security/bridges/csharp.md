---
from: csharp
status: draft
concept: "Least privilege and a minimal attack surface"
from_way: "An IIS application pool identity with only the permissions the site needs."
native_way: "Recent .NET images define a non-root app user (UID 1654); set USER app (or $APP_UID) and listen on port 8080, which the images use by default."
sensei_tip: "Chiseled .NET images have no shell and no package manager, and run as non-root out of the box: the closest .NET gets to distroless."
---

| C# / .NET | Image hardening |
| --- | --- |
| an application pool identity | `USER $APP_UID` |
| port 80 behind IIS | port 8080, no root needed |
| health checks middleware (`MapHealthChecks`) | the target of the health check |
| a server with the SDK installed | SDK only in the build stage |
