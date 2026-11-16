---
from: java
status: draft
concept: "Integrate continuously, release in small batches"
from_way: "Feature branches live for weeks, a release branch is cut, QA tests it for a sprint and a big-bang deploy follows."
native_way: "Everyone merges to main daily behind feature flags; each merge is built by Maven in CI and can be deployed."
sensei_tip: "Maven's -SNAPSHOT versions hide which build is in production; let CI stamp each artefact with the commit SHA instead."
---

| Java world | DevOps |
| --- | --- |
| a release branch per quarter | trunk-based development |
| `1.4.0-SNAPSHOT` | an immutable build tagged with the commit |
| a QA phase | automated tests in the pipeline |
| a release manager | a deployment pipeline |
