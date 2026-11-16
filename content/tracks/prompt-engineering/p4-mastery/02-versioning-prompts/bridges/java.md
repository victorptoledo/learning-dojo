---
from: java
status: draft
concept: "Prompts as versioned artefacts"
from_way: "Libraries with semantic versions in a pom.xml, database migrations with Flyway, and release notes."
native_way: "Each prompt lives in its own files with an id and a version that pins the template, model snapshot, parameters, tools and schema, with a changelog and an evaluation result per change."
sensei_tip: "Pinning a model snapshot is like pinning a dependency version in Maven instead of using a version range: reproducible builds need fixed inputs."
---

| Java | Prompt-versioning concept |
| --- | --- |
| `<version>2.3.0</version>` | a prompt version |
| a version range `[2.0,)` | a moving model alias (avoid) |
| Flyway migrations reviewed in PRs | prompt files reviewed in PRs |
| `CHANGELOG.md` | why each prompt version changed |
| Togglz or Unleash flags | selecting a prompt version at run time |
