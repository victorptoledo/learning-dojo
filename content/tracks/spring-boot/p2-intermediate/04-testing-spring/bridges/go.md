---
from: go
status: draft
concept: "Testing with Spring Boot 4"
from_way: "httptest.NewRecorder, table tests and interfaces with fakes."
native_way: "MockMvc, slices and the container in tests."
sensei_tip: "httptest.NewRecorder with your handler is a MockMvc call. Keep most logic testable without Spring, like Go code, and use the container only at the edges."
---

| Go | Spring Boot 4 |
| --- | --- |
| httptest.NewRecorder | MockMvc |
| fake struct implementing an interface | a fake or a @MockitoBean |
| testcontainers-go | Testcontainers |
| table tests | @ParameterizedTest |
