---
from: go
status: draft
concept: "One definition, many environments"
from_way: "text/template renders configuration files in a deploy tool."
native_way: "Helm templates are Go text/template with the Sprig functions; Kustomize avoids templates and patches structured YAML."
sensei_tip: "Helm is a Go program and its templates are text/template: everything you know about pipelines, range and with applies directly."
---

| Go world | Kubernetes |
| --- | --- |
| `text/template` | Helm templates |
| a struct of settings | `.Values` |
| `go.mod` versions | chart versions and dependencies |
| build tags per environment | Kustomize overlays |
