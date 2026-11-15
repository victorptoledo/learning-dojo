---
from: python
status: draft
concept: "Fast startup for Spring services"
from_way: "Interpreter startup in milliseconds, but slow imports for big frameworks."
native_way: "JVM startup in seconds for large apps, shortened with the AOT cache, CRaC or a GraalVM native image."
sensei_tip: "For scale-to-zero, a native image starts in tens of milliseconds, like a small Python app, but builds take minutes and reflection must be known at build time."
---

| Python | Spring Boot 4 |
| --- | --- |
| python startup | JVM startup (seconds) or native (ms) |
| lazy imports | spring.main.lazy-initialization |
| PyInstaller | native image |
| pre-forked gunicorn | CRaC checkpoint |
