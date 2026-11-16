---
from: java
status: draft
concept: "Governance, style guides and review"
from_way: "Each team's own conventions, enforced (or not) in code review of the controllers."
native_way: "A shared OpenAPI style guide checked by Spectral in the Maven/Gradle build, ArchUnit rules for code conventions, and a contract review before the controllers are written."
sensei_tip: "ArchUnit can enforce code-side rules (\"controllers return DTOs, never `@Entity` types\"); Spectral enforces the contract side. Together they cover both halves of the guide."
---

| Java / Spring | Governance concept |
| --- | --- |
| Spectral ruleset in CI | automated style guide |
| ArchUnit tests | code conventions |
| shared `problem` and `page` DTO library | consistency by reuse |
| pull request with `openapi.yaml` diff | design review |
| Backstage catalog entry | API catalogue |
