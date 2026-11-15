---
from: javascript
status: draft
concept: "Structuring a Spring application"
from_way: "NestJS modules with exports, or folders per feature."
native_way: "Modules with explicit public APIs and events, checked by Spring Modulith."
sensei_tip: "A NestJS module exports what others may use; in Spring Modulith the module's base package is public and subpackages are internal unless declared otherwise."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| @Module({ exports: [...] }) | the module's public API package |
| EventEmitter2 events | application events |
| nx enforce-module-boundaries | Spring Modulith tests |
| a microservice per module | extract later, if needed |
