---
from: javascript
status: draft
concept: "Testing with Spring Boot 4"
from_way: "jest with supertest for Express, or NestJS's Test.createTestingModule."
native_way: "JUnit with MockMvc, test slices and Mockito."
sensei_tip: "supertest(app).get('/books') is mockMvc.perform(get(\"/books\")). NestJS overrideProvider is @MockitoBean."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| supertest | MockMvc |
| overrideProvider | @MockitoBean |
| jest.fn() | Mockito.mock / when(...) |
| an in-memory database | @DataJpaTest with H2 or Testcontainers |
