---
from: java
status: draft
concept: "Deployment automation"
from_way: "An old CI server with manual build scripts and someone clicking a button to release."
native_way: "GitOps: the Git repository is the single source of truth. A change to `main` runs lint and tests, tags a release and the cluster syncs itself."
sensei_tip: "Fail fast: run formatting and unit tests in the first minute, before any heavy container build."
---
