---
from: csharp
status: draft
concept: "Run only what is needed, with only the rights it needs"
from_way: "IIS or Kestrel running as an administrator account with the default site and every feature enabled."
native_way: "A dedicated low-privilege app pool or container user, Kestrel behind a reverse proxy, unused IIS modules removed, and regular patching."
sensei_tip: "The .NET container images since .NET 8 include a non-root `app` user (`USER $APP_UID`); use it and the chiseled images to cut the attack surface."
---

| C# | Hardened deployment |
| --- | --- |
| app pool as LocalSystem | ApplicationPoolIdentity or a gMSA |
| root in containers | USER $APP_UID, chiseled images |
| detailed errors enabled | custom errors in production |
| remote desktop open to the internet | a bastion or VPN |
| manual Windows updates | managed patching |
