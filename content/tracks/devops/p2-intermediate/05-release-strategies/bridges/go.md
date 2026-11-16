---
from: go
status: draft
concept: "Expose a new version to a few users first"
from_way: "You replace the binary on all hosts and restart them one by one."
native_way: "A canary Deployment runs the new binary for a share of the traffic; a controller such as Argo Rollouts steps the weight up from Prometheus queries."
sensei_tip: "Expose the version as a metric label (a build_info gauge) so dashboards can compare canary and stable directly."
---

| Go world | Release strategies |
| --- | --- |
| restart host by host | a rolling update |
| `build_info{version=...}` | comparing canary and stable |
| an env var switch | a feature flag |
| keeping the old binary | blue-green |
