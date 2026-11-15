---
from: javascript
status: draft
concept: "From a web framework to Spring Boot 4"
from_way: "Express is minimal and you choose every piece; NestJS adds modules, decorators and dependency injection."
native_way: "Spring Boot: starters, auto-configuration and an IoC container, close to NestJS but older and broader."
sensei_tip: "If you know NestJS you know the shape: @Controller, @Injectable services, modules. Spring adds auto-configuration, which creates beans for whatever library is on the classpath."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| npx @nestjs/cli new | start.spring.io |
| @Module | @SpringBootApplication and component scanning |
| @Injectable() | @Service / @Component |
| app.listen(3000) | server.port=8080 |
| nodemon | spring-boot-devtools |
