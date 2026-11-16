---
from: go
status: draft
concept: "Give teams paved roads instead of a box of parts"
from_way: "A company Go module with logging, metrics and HTTP middleware that every service imports."
native_way: "That module becomes one piece of a golden path, next to a template repository, reusable pipelines and infrastructure modules."
sensei_tip: "Platform teams often build their APIs as Kubernetes controllers in Go: a CRD such as Database becomes self-service infrastructure."
---

| Go world | Platform engineering |
| --- | --- |
| a shared internal module | golden-path building blocks |
| `gonew` templates | software templates |
| a CLI for the team | a platform interface |
| a Kubernetes operator | self-service infrastructure |
