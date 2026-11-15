---
from: csharp
status: draft
concept: "Securing where your application runs"
from_way: "ASP.NET Core authentication and HTTPS redirection, Windows Update and the Windows firewall."
native_way: "The same layers on Linux: sshd, nftables or ufw, unattended-upgrades, AppArmor and systemd sandboxing."
sensei_tip: "dotnet user-secrets is for development only. In production give the service its secrets through an EnvironmentFile at mode 600, LoadCredential=, or a vault, never appsettings.json."
---

| C# | Linux |
| --- | --- |
| Windows Firewall | nftables or ufw |
| Windows Update | unattended-upgrades |
| appsettings.json secrets | LoadCredential= or a vault |
| Event Viewer > Security | journalctl -u ssh, journalctl _COMM=sudo |
| gMSA service account | User= or DynamicUser= |
