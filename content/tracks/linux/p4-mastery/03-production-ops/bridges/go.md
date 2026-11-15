---
from: go
status: draft
concept: "Operating what you deployed"
from_way: "go.sum pinning, lumberjack for log rotation, robfig/cron, and a binary copied to servers."
native_way: "The system-level practices around the binary: packages, logrotate, timers, backups, configuration as code."
sensei_tip: "Ship the binary as a .deb (nfpm or goreleaser can build one) and you get apt's upgrades, rollbacks and file lists for your own service, like every other package on the server."
---

| Go | Linux |
| --- | --- |
| go.sum | apt-mark hold, pinned versions |
| lumberjack | logrotate or the journal |
| robfig/cron | cron or a systemd timer |
| scp the binary | a .deb package, Ansible or an image |
| embed migrations | an explicit migration step in the deploy |
