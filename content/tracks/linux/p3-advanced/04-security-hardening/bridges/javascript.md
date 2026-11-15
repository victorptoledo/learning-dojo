---
from: javascript
status: draft
concept: "Securing where your application runs"
from_way: "helmet and rate-limit middleware in Express, npm audit, and the server left to its defaults."
native_way: "The server hardened too: SSH, firewall, updates, least privilege and logs."
sensei_tip: "npm install runs postinstall scripts with your user's rights. Build in CI, run the app as an unprivileged user, and keep the server's own packages patched with unattended-upgrades."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| helmet() | nftables and TLS at the proxy |
| npm audit | unattended-upgrades |
| process.env secrets | EnvironmentFile at 600 or LoadCredential= |
| node as root | User=node and a sandboxed unit |
