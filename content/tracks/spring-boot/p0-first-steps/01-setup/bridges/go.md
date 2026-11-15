---
from: go
status: draft
concept: "From a web framework to Spring Boot 4"
from_way: "net/http from the standard library, maybe chi or gin, and wiring done by hand in main."
native_way: "Spring Boot does the wiring: an IoC container builds and connects your objects from annotations and auto-configuration."
sensei_tip: "Go makes everything explicit in main.go; Spring hides it in the container. When something is missing, the actuator's beans and conditions endpoints show what was configured and why."
---

| Go | Spring Boot 4 |
| --- | --- |
| main.go wiring | the application context |
| http.ListenAndServe | embedded Tomcat started by Boot |
| gin or chi | Spring MVC |
| environment flags | application.properties and environment variables |
