---
from: python
status: draft
concept: "Evolutionary architecture"
from_way: "A rewrite of a Django monolith in a new repository, to be switched on 'when it is ready'."
native_way: "`import-linter` contracts in CI with ignored imports as a baseline, pytest-benchmark and Locust thresholds, `pip-audit`, SLO alerts, feature flags for branch by abstraction, and a proxy or Django middleware that routes slices to the new service."
sensei_tip: "`import-linter`'s `ignore_imports` list is a baseline: add today's violations there, and review that the list only ever gets shorter."
---

| Python | Evolutionary idea |
| --- | --- |
| `import-linter` with `ignore_imports` | structural fitness function with a baseline |
| pytest-benchmark, Locust | performance fitness function |
| `pip-audit`, Bandit | security fitness function |
| Prometheus alerts on SLOs | continuous fitness function |
| feature flags (Unleash, django-waffle) | branch by abstraction |
| nginx routing, a Django middleware proxy | strangler fig facade |
