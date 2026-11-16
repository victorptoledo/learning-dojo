---
from: python
status: draft
concept: "Integrate continuously, release in small batches"
from_way: "Changes pile up on a branch until someone runs `fab deploy` before a demo."
native_way: "Every merge runs pytest in CI and produces an artefact; deploying is a routine job, many times a week."
sensei_tip: "A pytest suite that takes 40 minutes kills CI; parallelise with pytest-xdist and keep the slow tests in a later stage."
---

| Python world | DevOps |
| --- | --- |
| `tox` on a laptop | the same checks in CI |
| a version bump before each release | a build per commit |
| `fab deploy` | an automated deploy job |
| a long-lived `develop` branch | short-lived branches merged daily |
