---
from: java
status: draft
concept: "Automating checks around commits"
from_way: "Checkstyle and Spotless run in the Maven build, so problems appear only in CI or when someone remembers to run mvn verify."
native_way: "A pre-commit hook runs the fast part (spotless:check on staged files) before every commit; CI still runs the full build."
sensei_tip: "Keep the hook fast: mvn -q spotless:check takes seconds; mvn verify takes minutes and will be skipped with --no-verify within a week."
---

| Java tooling | Git hook |
| --- | --- |
| `mvn spotless:check` in CI | also in `pre-commit` |
| Checkstyle rule for messages | `commit-msg` hook |
| `mvn test` before pushing | `pre-push` |
| Maven plugin that installs hooks (e.g. git-build-hook) | sets `core.hooksPath` for every clone |
