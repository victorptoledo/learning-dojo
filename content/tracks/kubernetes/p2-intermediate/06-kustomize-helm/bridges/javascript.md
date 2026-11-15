---
from: javascript
status: draft
concept: "One definition, many environments"
from_way: "Config files merged per environment (config/default.json plus config/production.json)."
native_way: "Kustomize merges patches into a base the same way; Helm fills templates from values, like a templating engine."
sensei_tip: "Helm is to Kubernetes what npm is to Node: charts have versions, dependencies and a registry; pin chart versions like you pin package versions."
---

| Node.js world | Kubernetes |
| --- | --- |
| `config/production.json` overrides | a Kustomize overlay patch |
| npm packages | Helm charts |
| `package-lock.json` | `Chart.lock` and pinned `--version` |
| a template literal | a Helm template |
