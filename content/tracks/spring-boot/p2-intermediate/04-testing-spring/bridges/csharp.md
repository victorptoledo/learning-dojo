---
from: csharp
status: draft
concept: "Testing with Spring Boot 4"
from_way: "xUnit with WebApplicationFactory and Moq."
native_way: "JUnit, @SpringBootTest with MockMvc, slices and Mockito."
sensei_tip: "WebApplicationFactory<Program> is @SpringBootTest; ConfigureTestServices is @MockitoBean or @TestConfiguration; Moq is Mockito."
---

| C# | Spring Boot 4 |
| --- | --- |
| WebApplicationFactory<Program> | @SpringBootTest + @AutoConfigureMockMvc |
| ConfigureTestServices | @MockitoBean / @TestConfiguration |
| Moq | Mockito |
| EF Core InMemory provider | @DataJpaTest |
