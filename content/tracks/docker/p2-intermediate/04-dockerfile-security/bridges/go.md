---
from: go
status: draft
concept: "Least privilege and a minimal attack surface"
from_way: "A systemd unit with User=svc and NoNewPrivileges=yes for the binary."
native_way: "distroless/static:nonroot gives the same with nothing else in the image: UID 65532, no shell, only your static binary, certificates and time zones."
sensei_tip: "Add a -healthcheck flag that GETs your own /health and exits 0 or 1: it is the only health check that works in a shell-less image without extra binaries."
---

| Go | Image hardening |
| --- | --- |
| `User=svc` in systemd | `USER 65532` (or the `:nonroot` tag) |
| `NoNewPrivileges=yes` | `--security-opt no-new-privileges` (Phase 3) |
| a `-healthcheck` flag | `HEALTHCHECK CMD ["/app/server", "-healthcheck"]` |
| `govulncheck` | image scanning (Phase 3) |
