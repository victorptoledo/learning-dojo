---
from: java
status: draft
concept: "Build caches, build secrets and target platforms"
from_way: "Maven keeps ~/.m2 between local builds, credentials live in settings.xml, and the JVM makes the JAR run on any CPU."
native_way: "Mount ~/.m2 as a BuildKit cache, mount settings.xml as a build secret, and let buildx produce amd64 and arm64 images from the same JAR."
sensei_tip: "RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml mvn -B package keeps repository passwords out of every layer."
---

| Java | BuildKit |
| --- | --- |
| `~/.m2/repository` | `--mount=type=cache,target=/root/.m2` |
| `settings.xml` with credentials | `--mount=type=secret` |
| `-Drevision=1.4.0` | `ARG VERSION` passed to Maven and to a label |
| "write once, run anywhere" | still needs a base image per platform; `buildx --platform` |
