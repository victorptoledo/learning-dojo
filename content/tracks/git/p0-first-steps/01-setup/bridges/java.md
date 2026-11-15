---
from: java
status: draft
concept: "Version control in a Java project"
from_way: "IntelliJ's Local History or a shared network folder, with target/ and IDE files mixed in with the sources."
native_way: "A Git repository with a .gitignore for build output and IDE files and a .gitattributes for line endings, so only the sources, the pom.xml and the Maven wrapper are versioned."
sensei_tip: "Commit mvnw, mvnw.cmd and .mvn/wrapper, but give mvnw `eol=lf` in .gitattributes: a CRLF mvnw fails on Linux CI with a confusing bad interpreter error."
---

| Java habit | With Git |
| --- | --- |
| IntelliJ Local History | `git log`, `git diff`, real commits |
| `target/` copied around | `target/` in `.gitignore` |
| `.idea/`, `*.iml` | ignored, or only shared run configurations committed |
| Build version in `pom.xml` | a tag such as `v1.4.0` marks the release commit (Phase 3) |
