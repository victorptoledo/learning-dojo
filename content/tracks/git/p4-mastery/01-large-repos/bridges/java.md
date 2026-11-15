---
from: java
status: draft
concept: "Large repositories"
from_way: "Huge multi-module Maven builds where every developer checks out and builds every module."
native_way: "Sparse checkout of the modules you work on, plus a build that builds only what changed (mvn -pl module -am, or Gradle with a build cache)."
sensei_tip: "Never commit built JARs or WARs: every version stays in every clone forever. Publish them to Nexus or GitHub Packages instead."
---

| Java situation | Git technique |
| --- | --- |
| 200 Maven modules, you touch 3 | `git sparse-checkout set` those modules |
| CI builds one commit | `--depth 1` or `--filter=tree:0` |
| `lib/*.jar` committed years ago | rewrite with `git filter-repo`, then a repository manager |
| IntelliJ indexing a huge tree | sparse checkout shrinks what it indexes |
