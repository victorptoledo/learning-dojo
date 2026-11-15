---
from: java
status: draft
concept: "Team workflows"
from_way: "Gitflow with develop, release-x.y branches and the Maven Release Plugin, chosen by default for every project."
native_way: "Pick the workflow from how you ship: a Spring Boot service deployed continuously works best trunk-based; a library with supported versions keeps release branches."
sensei_tip: "Spring's own feature flags can be as simple as @ConditionalOnProperty or a FeatureFlags bean reading configuration. That is enough to merge half-finished work to main safely."
---

| Java habit | Workflow choice |
| --- | --- |
| `develop` + `release/*` for a web service | trunk-based, deploy `main` |
| a library with 2.x and 3.x users | release branches, cherry-pick fixes |
| `@ConditionalOnProperty("features.new-checkout")` | a feature flag |
| SonarQube quality gate | a required status check |
