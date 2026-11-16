---
from: java
status: reviewed
concept: "From a fat JAR to a promoted release"
from_way: "mvn package on the build server, copy the JAR to a VM, edit application.properties there per environment."
native_way: "Build one image per commit (Jib or buildpacks), configure it with environment variables and mounted secrets, run Flyway as a separate step, and promote the same image."
sensei_tip: "Spring Boot reads SPRING_DATASOURCE_PASSWORD or a configtree: mount of secret files, so the image never needs to change per environment."
---

| Java habit | Deployment practice |
| --- | --- |
| `application-prod.properties` in the JAR | environment variables and secrets at run time |
| Flyway on application start | a one-shot migration step before the release |
| rebuild for production | promote the tested image |
| Lambda with Spring | SnapStart or native images for cold starts |
