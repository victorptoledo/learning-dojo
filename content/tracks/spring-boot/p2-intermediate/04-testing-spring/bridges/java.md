---
from: java
status: draft
concept: "Testing with Spring Boot 4"
from_way: "@RunWith(SpringRunner.class) on every test, @SpringBootTest for everything, @MockBean, and a shared test database that tests clean by hand."
native_way: "Plain unit tests for rules, slices (@WebMvcTest, @DataJpaTest) for layers, @MockitoBean, cached contexts, and Testcontainers for the real database."
sensei_tip: "If a test needs @SpringBootTest only to get one service, the service probably has a dependency you could pass in by hand."
---

| Boot 2/3 habit | Boot 4 |
| --- | --- |
| `@RunWith(SpringRunner.class)` | not needed (JUnit Jupiter extension is built in) |
| `@MockBean`, `@SpyBean` | `@MockitoBean`, `@MockitoSpyBean` |
| `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `spring-boot-starter-test` for every slice | plus `spring-boot-starter-webmvc-test`, `-data-jpa-test`, ... |
| `TestRestTemplate` | `RestTestClient` |
