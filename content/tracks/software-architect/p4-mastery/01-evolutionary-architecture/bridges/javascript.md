---
from: javascript
status: draft
concept: "Evolutionary architecture"
from_way: "A big-bang rewrite from one framework to another, with the old app frozen for months."
native_way: "`dependency-cruiser` rules with a known-violations baseline in CI, Lighthouse CI and bundle-size budgets, `npm audit`, SLO alerts, feature flags, and a proxy or micro-frontend route that strangles the old app page by page."
sensei_tip: "`dependency-cruiser --ignore-known` reads a baseline of known violations and fails only on new ones. Generate it once with `depcruise-baseline` and let it shrink."
---

| JavaScript / TypeScript | Evolutionary idea |
| --- | --- |
| `dependency-cruiser` with `--ignore-known` | structural fitness function with a baseline |
| Lighthouse CI, `size-limit` budgets | performance fitness functions |
| `npm audit`, Socket, Snyk | security fitness function |
| SLO alerts from OpenTelemetry metrics | continuous fitness function |
| feature flags (OpenFeature, LaunchDarkly) | branch by abstraction |
| a reverse proxy routing per path | strangler fig facade |
