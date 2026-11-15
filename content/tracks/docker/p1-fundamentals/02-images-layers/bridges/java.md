---
from: java
status: draft
concept: "Layered, content-addressed artifacts"
from_way: "A fat JAR bundles your code and every dependency into one file, so a one-line change ships 80 MB again."
native_way: "An image is a stack of layers addressed by digest: put dependencies in an early layer and your classes in a late one, and a change ships only the small top layer."
sensei_tip: "Spring Boot can split a JAR into layers (dependencies, loader, snapshot dependencies, application) with java -Djarmode=tools; copy them in that order and rebuilds reuse the dependency layers."
---

| Java | Image |
| --- | --- |
| a Maven artifact `group:artifact:version` | `repository:tag` |
| a release version that never changes | a digest `@sha256:...` |
| a SNAPSHOT version | a tag that can move, such as `latest` |
| the local `~/.m2` cache | the engine's local layer store |
