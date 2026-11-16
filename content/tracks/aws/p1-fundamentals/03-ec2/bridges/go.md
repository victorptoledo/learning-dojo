---
from: go
status: draft
concept: "Instances are disposable; build them from images and give them roles"
from_way: "A static Go binary is copied to long-lived VMs and restarted by hand."
native_way: "User data downloads the binary (or the AMI has it) and starts a systemd unit; the ASG replaces failed instances."
sensei_tip: "Cross-compile with `GOARCH=arm64` for Graviton; a static Go binary is the easiest workload to move to Arm and save 20 % or more."
---

| Go world | AWS |
| --- | --- |
| `scp` and `systemctl restart` | instance refresh with a new launch template |
| a VM per service | an ASG per service, or containers (Phase 2) |
| a cron on one VM | EventBridge Scheduler |
| an env file with keys | the instance profile, read through IMDSv2 |
