---
from: java
status: draft
concept: "One immutable artefact for every environment"
from_way: "A fat JAR runs on whatever JDK the server has, with -D flags that differ per machine."
native_way: "The image carries the exact JRE and the JAR; environments differ only in configuration injected at start."
sensei_tip: "Use jlink or a JRE base image and Spring Boot's layered JARs so the dependency layer stays cached between builds."
---

| Java world | Containers |
| --- | --- |
| the server's JDK | the JRE inside the image |
| `java -jar app.jar` | the image's ENTRYPOINT |
| `-Dspring.profiles.active=prod` | an environment variable at run time |
| a Nexus repository | an image registry |
