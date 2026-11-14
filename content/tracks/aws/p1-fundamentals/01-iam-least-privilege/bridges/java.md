---
from: java
status: draft
concept: "Cloud access control"
from_way: "Hardcoding `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` in configuration files."
native_way: "Attach an IAM role to the workload (e.g. IRSA for pods) so it receives temporary, auto-rotated credentials."
sensei_tip: "Static keys leaked on GitHub are found by bots within minutes and can produce huge crypto-mining bills."
---
