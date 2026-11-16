---
from: csharp
status: draft
concept: "Security checks run in the delivery path"
from_way: "Connection strings with passwords in web.config, and a security review before major releases."
native_way: "Secrets come from Key Vault through managed identity, dotnet list package --vulnerable runs in CI, and CodeQL scans every pull request."
sensei_tip: "Use managed identity or workload identity federation; a connection string without a password cannot leak a password."
---

| C# world | DevSecOps |
| --- | --- |
| `web.config` secrets | Key Vault and managed identity |
| `dotnet list package --vulnerable` | SCA |
| Roslyn security analyzers | SAST |
| Authenticode signing | image signing with Cosign |
