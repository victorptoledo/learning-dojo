---
from: go
status: draft
concept: "Shipping changes"
from_way: "A binary built in CI and rolled out by the platform."
native_way: "Continuous delivery with progressive rollouts and compatible migrations."
sensei_tip: "Fast Go builds make small, frequent deploys easy; the database is what needs care, with expand-and-contract migrations."
---

| Go | In practice |
| --- | --- |
| golang-migrate up | expand, then contract later |
| new binary everywhere at once | canary first |
| build tags | feature flags at runtime instead |
| rollback the binary | possible only with a compatible schema |
