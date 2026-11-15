---
from: python
status: draft
concept: "Securing where your application runs"
from_way: "Django or FastAPI security middleware, with the server left to defaults and secrets in a .env file."
native_way: "Defence in depth one layer down: keys-only SSH, a default-deny firewall, security updates and a sandboxed service user."
sensei_tip: "A deserialisation bug (pickle, yaml.load) runs code as your service's user. A dedicated user, ProtectSystem=strict and secrets at mode 600 limit what that code can touch."
---

| Python | Linux |
| --- | --- |
| SECURE_* settings in Django | nftables default deny, TLS at nginx |
| pip-audit | unattended-upgrades for the system |
| .env file in the repository | EnvironmentFile at mode 600 or LoadCredential= |
| running uvicorn as root | User=app, NoNewPrivileges=yes |
