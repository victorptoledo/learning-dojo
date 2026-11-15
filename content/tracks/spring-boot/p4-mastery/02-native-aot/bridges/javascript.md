---
from: javascript
status: draft
concept: "Fast startup for Spring services"
from_way: "Node starts quickly and serverless platforms keep instances warm."
native_way: "A slower JVM start, mitigated by native images, the AOT cache or CRaC."
sensei_tip: "A Spring native image on a serverless platform behaves like a Node function: cold starts in milliseconds and low memory."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| Lambda cold start with Node | native image cold start |
| v8 snapshot | AOT cache / CRaC |
| esbuild tree shaking | native image reachability analysis |
