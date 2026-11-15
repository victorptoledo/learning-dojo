---
from: java
status: draft
concept: "Securing where your application runs"
from_way: "Spring Security protects the application, while the server is somebody else's problem: Windows Update, the Windows firewall and an administrator account."
native_way: "The same defence in depth one layer down: keys-only SSH, a default-deny firewall, unattended security updates, a dedicated user and a systemd sandbox around the JVM."
sensei_tip: "An exploit such as a deserialization or Log4Shell bug runs code as the JVM's user. With User=orders, ProtectSystem=strict and no outbound firewall exceptions it cannot write binaries or call home."
---

| Java / Windows | Linux |
| --- | --- |
| Spring Security filter chain | nftables input chain with policy drop |
| Windows Update | `unattended-upgrades` |
| Running the service as Administrator | `User=orders`, `NoNewPrivileges=yes` |
| Secrets in `application.properties` | an `EnvironmentFile` at mode 600 or `LoadCredential=` |
| Windows Event Viewer, Security log | `journalctl -u ssh`, `journalctl _COMM=sudo` |
| Java Security Manager (removed) | AppArmor or SELinux profiles |
