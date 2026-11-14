---
from: java
status: draft
concept: "From plain Java (or old Spring) to Spring Boot 4"
from_way: "Objects created with new and passed around by hand, or XML application contexts and a WAR deployed to Tomcat, with every library version chosen by you."
native_way: "Beans declared with annotations and injected through constructors, starters with managed versions, auto-configuration, and an executable JAR with an embedded server."
sensei_tip: "If you are fighting an auto-configuration, define your own bean of that type: Boot backs off. Run with --debug to see why something was configured."
---

| Plain Java / old Spring | Spring Boot 4 |
| --- | --- |
| `new OrderService(new OrderRepository(ds))` | `@Service` + constructor parameters |
| `applicationContext.xml` | `@Configuration` classes and auto-configuration |
| WAR in an external Tomcat | `java -jar app.jar` with embedded Tomcat |
| Choosing library versions by hand | `spring-boot-starter-parent` |
| `spring-boot-starter-web` (Boot 3) | `spring-boot-starter-webmvc` (Boot 4) |
| Jackson 2 (`com.fasterxml.jackson`) | Jackson 3 (`tools.jackson`) by default |
