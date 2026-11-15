---
from: java
status: draft
concept: "Versions and release tags"
from_way: "The version lives in pom.xml (1.5.0-SNAPSHOT) and the Maven Release Plugin commits, tags and bumps it."
native_way: "The tag is the source of truth for a release; the pom version can be derived from it at build time (Maven CI-friendly ${revision})."
sensei_tip: "With <version>${revision}</version> and mvn -Drevision=$(git describe --tags --abbrev=0 | sed 's/^v//') package, CI builds the version straight from the tag, with no release commits to bump the pom."
---

| Maven habit | Git |
| --- | --- |
| `1.5.0-SNAPSHOT` | `git describe --tags`: `v1.4.0-12-g3f9a2c1` |
| `mvn release:prepare` tags `shop-1.5.0` | `git tag -a v1.5.0 -m "Release 1.5.0"` |
| deploy to Nexus | push the tag, CI publishes |
| a fixed version must never change in the repository manager | a pushed tag must never move |
