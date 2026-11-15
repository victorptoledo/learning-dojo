---
from: python
status: draft
concept: "Wiring objects"
from_way: "Modules imported directly, FastAPI's Depends(), or passing objects by hand."
native_way: "Beans created by the container and injected through constructors."
sensei_tip: "FastAPI's Depends() is per-request injection in the function signature; Spring injects singletons into constructors once, at startup. Keep beans stateless, like module-level services."
---

| Python | Spring Boot 4 |
| --- | --- |
| Depends(get_db) | constructor parameter OrderRepository repo |
| module-level singleton | a @Service bean (singleton) |
| app.dependency_overrides | @MockitoBean or test configuration |
| factory function | @Bean method |
