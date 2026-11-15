---
from: go
status: draft
concept: "Running services in production"
from_way: "A binary copied to servers with a systemd unit, flags for configuration and a restart on deploy."
native_way: "The binary ships as an image promoted by digest; readiness flips to false on SIGTERM before http.Server.Shutdown drains, so the router stops sending traffic first."
sensei_tip: "On SIGTERM, fail readiness, wait a few seconds for load balancers to notice, then call Shutdown; that order removes the last errors of a rolling update."
---

| Go | Production containers |
| --- | --- |
| `scp` and `systemctl restart` | push and roll out a digest |
| flags per environment | environment variables in one image |
| `signal.NotifyContext` | graceful drain within the grace period |
| `/healthz` and `/readyz` | liveness and readiness probes |
