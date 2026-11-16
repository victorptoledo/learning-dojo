---
from: python
status: draft
concept: "Expose a new version to a few users first"
from_way: "You deploy to all servers and watch Sentry nervously."
native_way: "One canary receives 5% of requests; its error rate and latency are compared with stable before more traffic moves."
sensei_tip: "Django or Alembic migrations that rename a column break the old code still running; add the new one first and drop the old one a release later."
---

| Python world | Release strategies |
| --- | --- |
| deploy to all | a rolling update |
| a waffle or flag package | a feature flag |
| `ALTER TABLE ... RENAME` | expand and contract |
| watching Sentry | automated canary analysis |
