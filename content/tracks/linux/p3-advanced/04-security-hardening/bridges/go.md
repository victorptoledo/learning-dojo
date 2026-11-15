---
from: go
status: draft
concept: "Securing where your application runs"
from_way: "A small static binary with few dependencies and govulncheck in CI."
native_way: "The server around the binary hardened: SSH, firewall, updates and systemd sandboxing."
sensei_tip: "A static Go binary needs almost nothing from the system, which makes strict sandboxing easy: DynamicUser=yes, ProtectSystem=strict, PrivateTmp=yes and an empty CapabilityBoundingSet usually just work."
---

| Go | Linux |
| --- | --- |
| govulncheck ./... | unattended-upgrades for the system |
| crypto/tls | TLS at the binary or the proxy |
| os.Getenv secrets | LoadCredential=, read from $CREDENTIALS_DIRECTORY |
| a FROM scratch image | a minimal attack surface, like a sandboxed unit |
