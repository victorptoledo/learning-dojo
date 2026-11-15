---
from: go
status: draft
concept: "Wiring objects"
from_way: "Explicit wiring in main, sometimes Wire or fx for generated or runtime DI."
native_way: "The container creates and injects beans for you."
sensei_tip: "Uber's fx is the closest Go thing to Spring's container. In Spring, write constructors as you would in Go (final fields, no setters) and let the container call them."
---

| Go | Spring Boot 4 |
| --- | --- |
| NewOrderService(repo, clock) | constructor of a @Service |
| main.go wiring | component scanning |
| google/wire | the container at startup |
| interface for tests | interface or @MockitoBean |
