---
from: java
status: draft
concept: "Sharing code between repositories"
from_way: "A shared library built and deployed to Nexus as a Maven artifact with a version."
native_way: "That is still the best default for Java. Submodules fit only when the code cannot be published, for example a shared proto or a vendored fork."
sensei_tip: "IntelliJ opens a worktree as a separate project, with its own target/ and index. Use one to keep a long mvn verify running on main while you code in the other."
---

| Java habit | Git option |
| --- | --- |
| `<dependency>` with a version | a package (preferred) |
| a multi-module build sharing code | a monorepo, no submodules needed |
| a second IntelliJ checkout to review | `git worktree add ../review feature/x` |
| a vendored fork of a library | a submodule pinned to your fork's commit |
