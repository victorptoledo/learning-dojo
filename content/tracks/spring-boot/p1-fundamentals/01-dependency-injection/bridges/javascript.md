---
from: javascript
status: draft
concept: "Wiring objects"
from_way: "NestJS providers with @Injectable and constructor injection, or plain imports in Express."
native_way: "Spring beans: @Component and @Service classes, constructor injection, @Bean factory methods."
sensei_tip: "NestJS DI is modelled on Angular and Spring. providers: [OrderService] is component scanning; useFactory is a @Bean method."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| @Injectable() | @Service |
| constructor(private repo: Repo) | constructor injection |
| providers: [{ provide, useFactory }] | @Bean method |
| Test.createTestingModule | @SpringBootTest or plain new in unit tests |
