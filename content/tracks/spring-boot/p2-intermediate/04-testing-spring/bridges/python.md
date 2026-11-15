---
from: python
status: draft
concept: "Testing with Spring Boot 4"
from_way: "pytest with FastAPI's TestClient, fixtures and dependency overrides."
native_way: "Plain unit tests, slices such as @WebMvcTest and @DataJpaTest, and full @SpringBootTest runs."
sensei_tip: "FastAPI's TestClient is MockMvc; dependency_overrides is @MockitoBean. Spring caches test contexts, so keep their configuration consistent to stay fast."
---

| Python | Spring Boot 4 |
| --- | --- |
| TestClient(app) | MockMvc |
| app.dependency_overrides | @MockitoBean |
| pytest-django db fixture | @DataJpaTest |
| unittest.mock | Mockito |
| testcontainers-python | Testcontainers |
