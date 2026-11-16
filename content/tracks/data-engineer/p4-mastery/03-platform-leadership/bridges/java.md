---
from: java
status: draft
concept: "Platform thinking"
from_way: "Choosing frameworks for one service and writing internal libraries."
native_way: "Designing a data platform of managed and built pieces, with ownership, contracts and governance across teams."
sensei_tip: "A good internal platform is like a good framework: opinionated defaults (Spring Boot starters) that make the right way the easy way; data platforms need the same for ingestion, models and access."
---

| Java | Data platform leadership |
| --- | --- |
| a Spring Boot starter | a platform template for new pipelines |
| an internal BOM of versions | platform standards |
| an API contract (OpenAPI) | a data contract |
| an ADR in the repository | a recorded platform decision |
| microservices ownership | domain ownership in a data mesh |
